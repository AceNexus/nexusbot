package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.linecorp.bot.model.message.Message;

/**
 * 提醒功能 Facade - 協調提醒業務流程
 */
public interface ReminderFacade {

    Message showMenu();

    Message startCreation(String roomId);

    Message listActive(String roomId);

    Message showTodayLogs(String roomId);

    Message deleteReminder(Long reminderId, String roomId);

    void confirmReminder(Long reminderId, String roomId);

    void sendNotification(Reminder reminder, String enhancedContent);

    Message handleInteraction(String roomId, String messageText, String replyToken);
}
