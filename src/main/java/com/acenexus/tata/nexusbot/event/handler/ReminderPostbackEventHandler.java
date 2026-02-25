package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.ReminderFacade;
import com.acenexus.tata.nexusbot.facade.TimezoneFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_REMINDER_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_TIMEZONE_CHANGE;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANGE_TIME;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANGE_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_BOTH;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_LINE;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.DELETE_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.LIST_REMINDERS;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_COMPLETED;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_DAILY;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_ONCE;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_WEEKLY;
import static com.acenexus.tata.nexusbot.constants.Actions.TODAY_REMINDERS;

/**
 * 處理提醒相關的 Postback 事件。
 * 同時負責 CHANGE_TIMEZONE / CONFIRM_TIMEZONE / CANCEL_TIMEZONE_CHANGE
 * 在「提醒建立流程」與「獨立時區設定」兩種情境下的路由：
 * 流程中 → 委派 {@link ReminderFacade}；流程外 → 委派 {@link TimezoneFacade}。
 * 路由判斷放在 {@link #handle} 而非 {@link #canHandle}，確保 canHandle 維持純函數。
 */
@Component
@RequiredArgsConstructor
public class ReminderPostbackEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReminderPostbackEventHandler.class);

    private final ReminderFacade reminderFacade;
    private final TimezoneFacade timezoneFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.POSTBACK) {
            return false;
        }

        // 靜態動作
        if (event.hasAction(REMINDER_MENU, ADD_REMINDER, LIST_REMINDERS, TODAY_REMINDERS,
                REPEAT_ONCE, REPEAT_DAILY, REPEAT_WEEKLY,
                CHANNEL_LINE, CHANNEL_EMAIL, CHANNEL_BOTH,
                CANCEL_REMINDER_INPUT, CHANGE_TIME, CHANGE_TIMEZONE, CONFIRM_TIMEZONE, CANCEL_TIMEZONE_CHANGE)) {
            return true;
        }

        // 動態動作（包含參數）
        return event.actionStartsWith(DELETE_REMINDER) || event.actionStartsWith(REMINDER_COMPLETED);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String action = event.getPayloadString("action");
        String roomId = event.getRoomId();

        logger.info("ReminderPostbackEventHandler handling action: {} for room: {}", action, roomId);

        // 處理動態動作
        if (event.actionStartsWith(DELETE_REMINDER)) {
            return handleDeleteReminder(event, roomId);
        }
        if (event.actionStartsWith(REMINDER_COMPLETED)) {
            return handleReminderCompleted(event, roomId);
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
            case CHANGE_TIME -> reminderFacade.startTimeChange(roomId);
            case CHANGE_TIMEZONE -> reminderFacade.isInReminderFlow(roomId)
                    ? reminderFacade.startTimezoneChange(roomId)
                    : timezoneFacade.startChangingTimezone(roomId);
            case CONFIRM_TIMEZONE ->
                    reminderFacade.isInReminderFlow(roomId) ? reminderFacade.confirmTimezoneChange(roomId) : timezoneFacade.confirmTimezoneChange(roomId);
            case CANCEL_TIMEZONE_CHANGE ->
                    reminderFacade.isInReminderFlow(roomId) ? reminderFacade.cancelTimezoneChange(roomId) : timezoneFacade.cancelTimezoneChange(roomId);
            default -> null;
        };
    }

    private Message handleDeleteReminder(LineBotEvent event, String roomId) {
        Long reminderId = event.getPayloadParameterAsLong("action", "id");
        return reminderFacade.deleteReminder(reminderId, roomId);
    }

    private Message handleReminderCompleted(LineBotEvent event, String roomId) {
        Long reminderId = event.getPayloadParameterAsLong("action", "id");
        return reminderFacade.confirmReminder(reminderId, roomId);
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
