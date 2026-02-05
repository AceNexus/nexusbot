package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 提醒排程器
 * 每秒掃描一次，發送到期的提醒並處理重複邏輯
 * - 只發送已到期（reminderTimeInstant <= now）的提醒，絕不提前發送
 * - 通知邏輯完全委派給 ReminderNotificationService
 * - 排程器專注於「何時發送」，通知服務負責「如何發送」
 */
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final ReminderProcessor reminderProcessor;

    /**
     * 每秒執行一次，掃描並發送到期提醒
     * 精準到秒級，且只發送已到期的提醒
     */
    @Scheduled(fixedRate = 1000)
    public void processReminders() {
        try {
            List<Reminder> dueReminders = findDueReminders();

            if (dueReminders.isEmpty()) {
                return;
            }

            logger.info("Found {} due reminders", dueReminders.size());

            for (Reminder reminder : dueReminders) {
                try {
                    reminderProcessor.processReminder(reminder);  // 委派給處理器，確保 @Transactional 有效
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
        // 上限：now + 1ms 搭配 < 查詢，等同 <= now
        // 不再設置 5 分鐘下限，確保停機期間錯過的提醒在啟動後能被補發（由 Processor 處理重複邏輯推進）
        long endMillis = now.plusMillis(1).toEpochMilli();

        return reminderRepository.findDueRemindersByInstantBefore(endMillis);
    }
}