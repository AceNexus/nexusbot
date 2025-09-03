package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.Reminder;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderService {

    /**
     * 新增提醒
     */
    Reminder createReminder(String roomId, String content, LocalDateTime reminderTime,
                            String repeatType, String createdBy);

    /**
     * 查詢聊天室的所有活躍提醒
     */
    List<Reminder> getActiveReminders(String roomId);
}