package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.lock.DistributedLock;
import com.acenexus.tata.nexusbot.notification.ReminderNotificationService;
import com.acenexus.tata.nexusbot.util.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 提醒處理器，負責協調單個提醒的完整執行流程：
 * 分散式鎖定 → 重複邏輯更新（委派 {@link ReminderRepeatHandler}）→ 非同步通知發送。
 */
@Component
@RequiredArgsConstructor
public class ReminderProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReminderProcessor.class);

    private final DistributedLock distributedLock;
    private final ReminderRepeatHandler reminderRepeatHandler;
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

            // 1. 先更新 DB 狀態（委派給獨立 bean，@Transactional 透過 Proxy 正確生效）
            reminderRepeatHandler.handle(reminder);

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

}
