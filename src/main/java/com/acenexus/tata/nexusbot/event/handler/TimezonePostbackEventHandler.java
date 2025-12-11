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

import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_TIMEZONE_CHANGE;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANGE_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.KEEP_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.TIMEZONE_SETTINGS;

/**
 * 處理時區設定相關的 Postback 事件
 */
@Component
@RequiredArgsConstructor
public class TimezonePostbackEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(TimezonePostbackEventHandler.class);

    private final TimezoneFacade timezoneFacade;
    private final ReminderFacade reminderFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.POSTBACK) {
            return false;
        }

        // 檢查是否為時區設定相關動作
        if (!event.hasAction(TIMEZONE_SETTINGS, KEEP_TIMEZONE, CHANGE_TIMEZONE, CONFIRM_TIMEZONE, CANCEL_TIMEZONE_CHANGE)) {
            return false;
        }

        // 如果用戶正在提醒建立流程中，這些動作應該由 ReminderPostbackEventHandler 處理
        if (event.hasAction(CHANGE_TIMEZONE, CONFIRM_TIMEZONE, CANCEL_TIMEZONE_CHANGE)) {
            String roomId = event.getRoomId();
            boolean inReminderFlow = reminderFacade.isInReminderFlow(roomId);
            if (inReminderFlow) {
                logger.debug("User is in reminder flow, skipping timezone handler for action: {}", event.getPayloadString("action"));
                return false;
            }
        }

        return true;
    }

    @Override
    public Message handle(LineBotEvent event) {
        String action = event.getPayloadString("action");
        String roomId = event.getRoomId();

        logger.info("TimezonePostbackEventHandler handling action: {} for room: {}", action, roomId);

        return switch (action) {
            case TIMEZONE_SETTINGS -> timezoneFacade.showSettings(roomId);
            case KEEP_TIMEZONE -> timezoneFacade.keepTimezone(roomId);
            case CHANGE_TIMEZONE -> timezoneFacade.startChangingTimezone(roomId);
            case CONFIRM_TIMEZONE -> timezoneFacade.confirmTimezoneChange(roomId);
            case CANCEL_TIMEZONE_CHANGE -> timezoneFacade.cancelTimezoneChange(roomId);
            default -> null;
        };
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
