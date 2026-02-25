package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

/**
 * 提醒重複邏輯處理器。
 */
@Component
@RequiredArgsConstructor
public class ReminderRepeatHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderRepeatHandler.class);

    private final ReminderRepository reminderRepository;

    /**
     * 根據重複類型更新提醒狀態：
     * - ONCE   → 標記為 COMPLETED
     * - DAILY  → 推進至下一個每日時間點
     * - WEEKLY → 推進至下一個每週時間點
     * 整個操作在同一個事務內完成，確保狀態更新的原子性。
     */
    @Transactional
    public void handle(Reminder reminder) {
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
     * 推進重複提醒的下次觸發時間。
     * 若因停機導致下次時間仍在過去，持續推進直到進入未來（自愈機制）。
     */
    private void updateNextReminderTime(Reminder reminder, long amount, ChronoUnit unit) {
        String timezone = reminder.getTimezone() != null ? reminder.getTimezone() : "Asia/Taipei";
        ZoneId zoneId = ZoneId.of(timezone);

        ZonedDateTime nextZonedTime = reminder.getLocalTime().atZone(zoneId).plus(amount, unit);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        while (nextZonedTime.isBefore(now)) {
            nextZonedTime = nextZonedTime.plus(amount, unit);
        }

        reminder.setReminderTimeInstant(nextZonedTime.toInstant().toEpochMilli());
        reminderRepository.save(reminder);

        logger.info("Updated repeating reminder [{}]: next execution set to {} ({})", reminder.getId(), nextZonedTime.format(STANDARD_TIME), timezone);
    }
}
