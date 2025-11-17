package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.facade.ReminderFacade;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_REMINDER_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_BOTH;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_LINE;
import static com.acenexus.tata.nexusbot.constants.Actions.DELETE_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.LIST_REMINDERS;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_COMPLETED;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_DAILY;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_ONCE;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_WEEKLY;
import static com.acenexus.tata.nexusbot.constants.Actions.TODAY_REMINDERS;

/**
 * 提醒功能 Handler
 * 職責：純路由，將請求委派給 ReminderFacade
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class ReminderPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderPostbackHandler.class);

    private final ReminderFacade reminderFacade;

    @Override
    public boolean canHandle(String action) {
        // 靜態動作
        if (REMINDER_MENU.equals(action) || ADD_REMINDER.equals(action) ||
                LIST_REMINDERS.equals(action) || TODAY_REMINDERS.equals(action) ||
                REPEAT_ONCE.equals(action) || REPEAT_DAILY.equals(action) || REPEAT_WEEKLY.equals(action) ||
                CHANNEL_LINE.equals(action) || CHANNEL_EMAIL.equals(action) || CHANNEL_BOTH.equals(action) ||
                CANCEL_REMINDER_INPUT.equals(action)) {
            return true;
        }

        // 動態動作（包含參數）
        return action.startsWith(DELETE_REMINDER) || action.startsWith(REMINDER_COMPLETED);
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("ReminderPostbackHandler handling action: {} for room: {}", action, roomId);

        // 處理動態動作
        if (action.startsWith(DELETE_REMINDER)) {
            return handleDeleteReminder(action, roomId);
        }
        if (action.startsWith(REMINDER_COMPLETED)) {
            return handleReminderCompleted(action, roomId);
        }

        // 處理靜態動作
        return switch (action) {
            case REMINDER_MENU -> reminderFacade.showMenu();
            case ADD_REMINDER -> reminderFacade.startCreation(roomId);
            case LIST_REMINDERS -> reminderFacade.listActive(roomId);
            case TODAY_REMINDERS -> reminderFacade.showTodayLogs(roomId);
            case REPEAT_ONCE -> reminderFacade.setRepeatTypeOnce(roomId);
            case REPEAT_DAILY -> reminderFacade.setRepeatTypeDaily(roomId);
            case REPEAT_WEEKLY -> reminderFacade.setRepeatTypeWeekly(roomId);
            case CHANNEL_LINE -> reminderFacade.setNotificationChannelLine(roomId);
            case CHANNEL_EMAIL -> reminderFacade.setNotificationChannelEmail(roomId);
            case CHANNEL_BOTH -> reminderFacade.setNotificationChannelBoth(roomId);
            case CANCEL_REMINDER_INPUT -> reminderFacade.cancelCreation(roomId);
            default -> null;
        };
    }

    private Message handleDeleteReminder(String data, String roomId) {
        String idStr = data.substring(data.indexOf("&id=") + 4);
        Long reminderId = Long.parseLong(idStr);
        return reminderFacade.deleteReminder(reminderId, roomId);
    }

    private Message handleReminderCompleted(String data, String roomId) {
        String idStr = data.substring(data.indexOf("&id=") + 4);
        Long reminderId = Long.parseLong(idStr);
        return reminderFacade.confirmReminder(reminderId, roomId);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
