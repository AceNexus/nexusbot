package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.Reminder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderService {

    /**
     * 新增提醒（支援時區與 Instant）
     */
    Reminder createReminder(String roomId, String content, LocalDateTime reminderTime,
                            String timezone, Instant reminderInstant,
                            String repeatType, String createdBy, String notificationChannel);

    /**
     * 查詢聊天室的所有活躍提醒
     */
    List<Reminder> getActiveReminders(String roomId);

    /**
     * 刪除指定提醒
     */
    boolean deleteReminder(Long reminderId, String roomId);
}