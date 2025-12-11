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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

/**
 * 提醒排程器
 * 每分鐘掃描一次，發送到期的提醒並處理重複邏輯
 * - 通知邏輯完全委派給 ReminderNotificationService
 * - 排程器專注於「何時發送」，通知服務負責「如何發送」
 */
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final DistributedLock distributedLock;
    private final ReminderNotificationService reminderNotificationService;
    private final AIService aiService;

    /**
     * 每分鐘整分執行一次，掃描並發送到期提醒
     * cron: 每分鐘的第 0 秒執行（確保在整分時執行）
     */
    @Scheduled(cron = "0 * * * * *")
    public void processReminders() {
        try {
            List<Reminder> dueReminders = findDueReminders();

            if (dueReminders.isEmpty()) {
                return;
            }

            logger.info("Found {} due reminders", dueReminders.size());

            for (Reminder reminder : dueReminders) {
                try {
                    processReminderWithTransaction(reminder);  // 每個提醒獨立交易
                } catch (Exception e) {
                    logger.error("Failed to process reminder {}: {}", reminder.getId(), e.getMessage(), e);
                    // 繼續處理下一個提醒，不影響其他提醒
                }
            }

        } catch (Exception e) {
            logger.error("Reminder processing failed: {}", e.getMessage(), e);
        }
    }

    private List<Reminder> findDueReminders() {
        Instant now = Instant.now();
        Instant currentMinuteStart = now.truncatedTo(ChronoUnit.MINUTES);
        Instant nextMinuteStart = currentMinuteStart.plus(1, ChronoUnit.MINUTES);

        long startMillis = currentMinuteStart.toEpochMilli();
        long endMillis = nextMinuteStart.toEpochMilli();

        return reminderRepository.findDueRemindersByInstant(startMillis, endMillis);
    }

    @Transactional
    private void processReminderWithTransaction(Reminder reminder) {
        processReminder(reminder);  // 實際處理邏輯
    }

    /**
     * 處理單個提醒
     */
    @Transactional
    public void processReminder(Reminder reminder) {
        String lockKey = "reminder_" + reminder.getId();

        // 嘗試獲取分散式鎖，防止多實例重複處理
        if (!distributedLock.tryLock(lockKey)) {
            logger.debug("Reminder [{}] is already being processed by another instance", reminder.getId());
            return;
        }

        try {
            logger.info("Processing reminder [{}]: {}", reminder.getId(), reminder.getContent());

            // 1. 發送提醒訊息
            sendReminderMessage(reminder);

            // 2. 處理重複邏輯
            handleRepeatLogic(reminder);

            logger.info("Reminder [{}] completed", reminder.getId());

        } catch (Exception e) {
            logger.error("Failed to process reminder [{}]: {}", reminder.getId(), e.getMessage(), e);
        } finally {
            // 釋放分散式鎖
            distributedLock.releaseLock(lockKey);
        }
    }

    /**
     * 發送提醒訊息（非同步處理，委派給通知服務）
     * 支援 LINE、EMAIL、BOTH 三種通知管道
     */
    private void sendReminderMessage(Reminder reminder) {
        logger.info("Sending reminder [{}] for room [{}]: {}", reminder.getId(), reminder.getRoomId(), reminder.getContent());

        // 使用 MdcTaskDecorator 自動傳遞 traceId 到非同步執行緒
        CompletableFuture.runAsync(MdcTaskDecorator.wrap(() -> {
            try {
                // AI 增強提醒內容
                String enhancedContent = enhanceReminderWithAI(reminder.getContent());

                // 委派給通知服務處理
                reminderNotificationService.send(reminder, enhancedContent);

                logger.info("Reminder [{}] notification completed", reminder.getId());

            } catch (Exception e) {
                logger.error("Failed to send notification for reminder [{}]: {}", reminder.getId(), e.getMessage());
            }
        }));
    }

    /**
     * 用 AI 美化提醒內容，增加情感回復
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

            logger.warn("AI enhancement returned empty or unsuccessful response, fallback to original content.");
        } catch (Exception e) {
            logger.error("AI enhancement failed, using original content. Cause: {}", e.getMessage(), e);
        }

        // AI 失敗時返回原始內容
        return originalContent;
    }


    /**
     * 處理重複邏輯（支援時區）
     * ONCE: 標記為已完成
     * DAILY: 更新為明天同一時間（保持相同本地時間）
     * WEEKLY: 更新為下週同一時間（保持相同本地時間）
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
                // 使用時區計算下一次提醒時間
                updateNextReminderTime(reminder, 1, ChronoUnit.DAYS);
                logger.debug("Daily reminder [{}] updated to: {}", reminder.getId(), reminder.getLocalTime().format(STANDARD_TIME));
            }
            case "WEEKLY" -> {
                // 使用時區計算下一次提醒時間
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
     * 更新下一次提醒時間（保持本地時間，考慮時區）
     */
    private void updateNextReminderTime(Reminder reminder, long amount, ChronoUnit unit) {
        // 1. 取得原始時區與本地時間
        String timezone = reminder.getTimezone() != null ? reminder.getTimezone() : "Asia/Taipei";
        LocalDateTime currentLocalTime = reminder.getLocalTime();

        // 2. 轉換為 ZonedDateTime 並加上時間間隔
        ZonedDateTime currentZonedTime = currentLocalTime.atZone(ZoneId.of(timezone));
        ZonedDateTime nextZonedTime = currentZonedTime.plus(amount, unit);

        // 3. 更新 Instant（唯一的時間儲存欄位）
        Instant nextInstant = nextZonedTime.toInstant();
        reminder.setReminderTimeInstant(nextInstant.toEpochMilli());

        reminderRepository.save(reminder);

        logger.debug("Updated reminder [{}]: {} ({}) -> instant: {}", reminder.getId(), nextZonedTime.toLocalDateTime(), timezone, nextInstant);
    }
}