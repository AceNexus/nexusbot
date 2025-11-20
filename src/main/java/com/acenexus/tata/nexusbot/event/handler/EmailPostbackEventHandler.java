package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.EmailFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_EMAIL_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.DELETE_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.EMAIL_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_EMAIL_STATUS;

/**
 * 處理 Email 相關的 Postback 事件
 */
@Component
@RequiredArgsConstructor
public class EmailPostbackEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(EmailPostbackEventHandler.class);

    private final EmailFacade emailFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.POSTBACK) {
            return false;
        }

        // 靜態動作
        if (event.hasAction(EMAIL_MENU, ADD_EMAIL, CANCEL_EMAIL_INPUT)) {
            return true;
        }

        // 動態動作（包含參數）
        return event.actionStartsWith(DELETE_EMAIL) || event.actionStartsWith(TOGGLE_EMAIL_STATUS);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String action = event.getPayloadString("action");
        String roomId = event.getRoomId();

        logger.info("EmailPostbackEventHandler handling action: {} for room: {}", action, roomId);

        // 處理動態動作
        if (event.actionStartsWith(DELETE_EMAIL)) {
            return handleDeleteEmail(event, roomId);
        }

        if (event.actionStartsWith(TOGGLE_EMAIL_STATUS)) {
            return handleToggleEmailStatus(event, roomId);
        }

        // 處理靜態動作
        return switch (action) {
            case EMAIL_MENU -> emailFacade.showMenu(roomId);
            case ADD_EMAIL -> emailFacade.startAddingEmail(roomId);
            case CANCEL_EMAIL_INPUT -> emailFacade.cancelAddingEmail(roomId);
            default -> null;
        };
    }

    private Message handleDeleteEmail(LineBotEvent event, String roomId) {
        Long emailId = event.getPayloadParameterAsLong("action", "id");
        return emailFacade.deleteEmail(emailId, roomId);
    }

    private Message handleToggleEmailStatus(LineBotEvent event, String roomId) {
        Long emailId = event.getPayloadParameterAsLong("action", "id");
        return emailFacade.toggleEmailStatus(emailId, roomId);
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
