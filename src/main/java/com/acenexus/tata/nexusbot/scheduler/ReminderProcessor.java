package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.lock.DistributedLock;
import com.acenexus.tata.nexusbot.notification.ReminderNotificationService;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import com.acenexus.tata.nexusbot.util.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

/**
 * 提醒處理器
 * 負責單個提醒的執行邏輯，包含交易處理、鎖定機制與重複邏輯
 */
@Component
@RequiredArgsConstructor
public class ReminderProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReminderProcessor.class);

    private final ReminderRepository reminderRepository;
    private final DistributedLock distributedLock;
    private final ReminderNotificationService reminderNotificationService;
    private final AIService aiService;

    /**
     * 處理單個提醒（具備鎖定機制）
     * 執行順序：
     * - 取得分散式鎖
     * - 更新 DB 狀態（handleRepeatLogic）
     * - 啟動非同步通知
     * - 等待通知完成後才釋放鎖（防止多實例在通知進行中重複取鎖）
     * 不使用 @Transactional：各 Repository 操作已有自己的事務；
     * 且持有 DB 連線等待非同步通知完成會浪費連線資源。
     */
    public void processReminder(Reminder reminder) {
        String lockKey = "reminder_" + reminder.getId();

        // 嘗試獲取分散式鎖，防止多實例重複處理
        if (!distributedLock.tryLock(lockKey)) {
            logger.debug("Reminder [{}] is already being processed by another instance", reminder.getId());
            return;
        }

        CompletableFuture<Void> notificationFuture = null;
        try {
            logger.info("Processing reminder [{}]: {}", reminder.getId(), reminder.getContent());

            // 1. 先更新 DB 狀態（各自的 Repository 事務確保原子性）
            handleRepeatLogic(reminder);

            // 2. DB 狀態確認更新後，啟動非同步通知
            notificationFuture = buildNotificationFuture(reminder);

            logger.info("Reminder [{}] state updated, notification dispatched", reminder.getId());

        } catch (Exception e) {
            logger.error("Failed to process reminder [{}]: {}", reminder.getId(), e.getMessage(), e);
            throw e; // 讓 Scheduler 層捕捉並記錄，下次排程重試
        } finally {
            // 等待通知完成後再釋放鎖，確保鎖的保護範圍涵蓋整個通知流程
            awaitNotification(notificationFuture, reminder.getId());
            distributedLock.releaseLock(lockKey);
        }
    }

    /**
     * 建立非同步通知任務（AI 增強 + 發送）
     */
    private CompletableFuture<Void> buildNotificationFuture(Reminder reminder) {
        logger.info("Sending reminder [{}] for room [{}]: {}", reminder.getId(), reminder.getRoomId(), reminder.getContent());

        return CompletableFuture.runAsync(MdcTaskDecorator.wrap(() -> {
            try {
                String enhancedContent = enhanceReminderWithAI(reminder.getContent());
                reminderNotificationService.send(reminder, enhancedContent);
                logger.info("Reminder [{}] notification completed", reminder.getId());
            } catch (Exception e) {
                logger.error("Failed to send notification for reminder [{}]: {}", reminder.getId(), e.getMessage());
            }
        }));
    }

    /**
     * 等待通知任務完成，忽略任務內部異常（已在任務內部記錄）
     */
    private void awaitNotification(CompletableFuture<Void> future, Long reminderId) {
        if (future == null) {
            return;
        }
        try {
            future.join();
        } catch (CompletionException e) {
            logger.error("Notification future for reminder [{}] completed exceptionally: {}", reminderId, e.getMessage());
        }
    }

    /**
     * 用 AI 美化提醒內容
     */
    private String enhanceReminderWithAI(String originalContent) {
        String promptTemplate = """
                請將以下提醒改寫成適合長輩看的文字，
                語氣溫馨、體貼、禮貌，
                簡短不超過10字，帶點鼓勵或祝福，
                最後加上合適的 emoji。
                原內容：%s
                請只回覆改寫後的文字，勿附其他說明，保持自然。
                """;

        try {
            String prompt = String.format(promptTemplate, originalContent);
            AIService.ChatResponse response = aiService.chatWithContext("reminder_enhancement", prompt, "llama-3.1-8b-instant");

            if (response != null && response.success() && response.content() != null && !response.content().trim().isEmpty()) {
                return response.content().trim();
            }
        } catch (Exception e) {
            logger.error("AI enhancement failed, using original content. Cause: {}", e.getMessage());
        }
        return originalContent;
    }

    /**
     * 處理重複邏輯
     */
    @Transactional
    public void handleRepeatLogic(Reminder reminder) {
        switch (reminder.getRepeatType().toUpperCase()) {
            case "ONCE" -> {
                reminder.setStatus("COMPLETED");
                reminderRepository.save(reminder);
                logger.debug("One-time reminder [{}] completed", reminder.getId());
            }
            case "DAILY" -> {
                updateNextReminderTime(reminder, 1, ChronoUnit.DAYS);
                logger.debug("Daily reminder [{}] updated to: {}", reminder.getId(), reminder.getLocalTime().format(STANDARD_TIME));
            }
            case "WEEKLY" -> {
                updateNextReminderTime(reminder, 1, ChronoUnit.WEEKS);
                logger.debug("Weekly reminder [{}] updated to: {}", reminder.getId(), reminder.getLocalTime().format(STANDARD_TIME));
            }
            default -> {
                logger.warn("Unknown repeat type '{}' for reminder [{}], marking as completed", reminder.getRepeatType(), reminder.getId());
                reminder.setStatus("COMPLETED");
                reminderRepository.save(reminder);
            }
        }
    }

    /**
     * 更新下一次提醒時間（具備自動補救機制，確保下次時間在未來）
     */
    private void updateNextReminderTime(Reminder reminder, long amount, ChronoUnit unit) {
        String timezone = reminder.getTimezone() != null ? reminder.getTimezone() : "Asia/Taipei";
        ZoneId zoneId = ZoneId.of(timezone);

        ZonedDateTime nextZonedTime = reminder.getLocalTime().atZone(zoneId).plus(amount, unit);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // 如果計算出的下一次時間仍在過去（例如停機太久），持續推進直到進入未來
        while (nextZonedTime.isBefore(now)) {
            nextZonedTime = nextZonedTime.plus(amount, unit);
        }

        reminder.setReminderTimeInstant(nextZonedTime.toInstant().toEpochMilli());
        reminderRepository.save(reminder);

        logger.info("Updated repeating reminder [{}]: next execution set to {} ({})",
                reminder.getId(), nextZonedTime.format(STANDARD_TIME), timezone);
    }
}
