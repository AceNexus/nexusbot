package com.acenexus.tata.nexusbot.facade;

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

    Message confirmReminder(Long reminderId, String roomId);

    Message handleInteraction(String roomId, String messageText, String replyToken);

    boolean isInReminderFlow(String roomId);

    // 處理重複類型選擇
    Message setRepeatTypeOnce(String roomId);

    Message setRepeatTypeDaily(String roomId);

    Message setRepeatTypeWeekly(String roomId);

    // 處理通知管道選擇
    Message setNotificationChannelLine(String roomId);

    Message setNotificationChannelEmail(String roomId);

    Message setNotificationChannelBoth(String roomId);

    // 新增：取消建立提醒
    Message cancelCreation(String roomId);

    // 時區修改相關
    Message startTimezoneChange(String roomId);

    Message cancelTimezoneChange(String roomId);

    Message confirmTimezoneChange(String roomId);

    // 時間修改相關
    Message startTimeChange(String roomId);
}
