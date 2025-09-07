package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.lock.DistributedLock;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 每分鐘執行一次，掃描並發送到期提醒
     */
    @Scheduled(fixedRate = 60000)
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
     * 發送提醒訊息
     */
    private void sendReminderMessage(Reminder reminder) {
        logger.info("Room [{}] 提醒訊息：{}", reminder.getRoomId(), reminder.getContent());

        // TODO 發送 Line 通知

        // TODO 記錄日誌
        ReminderLog log = new ReminderLog();
        log.setReminderId(reminder.getId());
        log.setRoomId(reminder.getRoomId());
        log.setStatus("SENT");
        reminderLogRepository.save(log);
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