package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.Reminder;

import java.time.LocalDateTime;

public interface ReminderService {

    /**
     * 新增提醒
     */
    Reminder createReminder(String roomId, String content, LocalDateTime reminderTime,
                            String repeatType, String createdBy);
}