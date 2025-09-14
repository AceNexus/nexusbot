package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.lock.DistributedLock;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

/**
 * 提醒排程器
 * 每分鐘掃描一次，發送到期的提醒並處理重複邏輯
 */
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final DistributedLock distributedLock;
    private final LineMessagingClient lineMessagingClient;
    private final MessageTemplateProvider messageTemplateProvider;
    private final AIService aiService;

    /**
     * 每分鐘整分執行一次，掃描並發送到期提醒
     * cron: 每分鐘的第0秒執行（確保在整分時執行）
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Reminder> dueReminders = findDueReminders(now);

            if (dueReminders.isEmpty()) {
                return;
            }

            logger.info("Found {} due reminders", dueReminders.size());

            for (Reminder reminder : dueReminders) {
                processReminder(reminder);
            }

        } catch (Exception e) {
            logger.error("Reminder processing failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 查詢到期的提醒
     */
    private List<Reminder> findDueReminders(LocalDateTime now) {
        LocalDateTime start = now.withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(1);
        return reminderRepository.findDueReminders(start, end);
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
     * 發送提醒訊息（非同步處理，AI 情感回復）
     */
    private void sendReminderMessage(Reminder reminder) {
        logger.info("Room [{}] 提醒訊息：{}", reminder.getRoomId(), reminder.getContent());

        CompletableFuture.runAsync(() -> {
            try {
                String enhancedContent = enhanceReminderWithAI(reminder.getContent());
                Message reminderMessage = messageTemplateProvider.buildReminderNotification(enhancedContent, reminder.getRepeatType(), reminder.getId());
                PushMessage pushMessage = new PushMessage(reminder.getRoomId(), reminderMessage);
                lineMessagingClient.pushMessage(pushMessage);

                saveReminderLog(reminder, "SENT", null);
                logger.debug("Async notification sent successfully for reminder [{}]", reminder.getId());

            } catch (Exception e) {
                logger.error("Failed to send async notification for reminder [{}]: {}", reminder.getId(), e.getMessage());
                saveReminderLog(reminder, "FAILED", e.getMessage());
            }
        });
    }

    /**
     * 用 AI 美化提醒內容，增加情感回復
     */
    private String enhanceReminderWithAI(String originalContent) {
        String promptTemplate = """
                將以下提醒改寫給長輩，語氣溫馨、禮貌、體貼，最後加上適合的 emoji。
                文字請簡短，控制在 10 字以內，並帶鼓勵或祝福。
                原內容：%s
                請直接回覆改寫後的文字，不要其他說明。
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
     * 保存提醒日誌
     */
    private void saveReminderLog(Reminder reminder, String status, String errorMessage) {
        try {
            ReminderLog log = new ReminderLog();
            log.setReminderId(reminder.getId());
            log.setRoomId(reminder.getRoomId());
            log.setStatus(status);
            log.setErrorMessage(errorMessage);
            reminderLogRepository.save(log);

            logger.debug("Reminder log saved for reminder [{}] with status [{}]", reminder.getId(), status);

        } catch (Exception e) {
            logger.error("Failed to save reminder log for reminder [{}]: {}", reminder.getId(), e.getMessage(), e);
        }
    }


    /**
     * 處理重複邏輯
     * ONCE: 標記為已完成
     * DAILY: 更新為明天同一時間
     * WEEKLY: 更新為下週同一時間
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
                LocalDateTime nextTime = reminder.getReminderTime().plusDays(1);
                reminder.setReminderTime(nextTime);
                reminderRepository.save(reminder);
                logger.debug("Daily reminder [{}] updated to: {}", reminder.getId(), nextTime.format(STANDARD_TIME));
            }
            case "WEEKLY" -> {
                LocalDateTime nextTime = reminder.getReminderTime().plusWeeks(1);
                reminder.setReminderTime(nextTime);
                reminderRepository.save(reminder);
                logger.debug("Weekly reminder [{}] updated to: {}", reminder.getId(), nextTime.format(STANDARD_TIME));
            }
            default -> {
                logger.warn("Unknown repeat type '{}' for reminder [{}], marking as completed", reminder.getRepeatType(), reminder.getId());
                reminder.setStatus("COMPLETED");
                reminderRepository.save(reminder);
            }
        }
    }
}