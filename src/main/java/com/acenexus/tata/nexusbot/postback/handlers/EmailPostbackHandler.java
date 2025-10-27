package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.facade.EmailFacade;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_EMAIL_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.DELETE_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.EMAIL_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_EMAIL_STATUS;

/**
 * Email 功能 Handler - 處理 Email 管理、新增、刪除、啟用/停用
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class EmailPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(EmailPostbackHandler.class);

    private final EmailFacade emailFacade;

    @Override
    public boolean canHandle(String action) {
        // 靜態動作
        if (EMAIL_MENU.equals(action) || ADD_EMAIL.equals(action) || CANCEL_EMAIL_INPUT.equals(action)) {
            return true;
        }

        // 動態動作（包含參數）
        return action.startsWith(DELETE_EMAIL) || action.startsWith(TOGGLE_EMAIL_STATUS);
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("EmailPostbackHandler handling action: {} for room: {}", action, roomId);

        // 處理動態動作
        if (action.startsWith(DELETE_EMAIL)) {
            return handleDeleteEmail(action, roomId);
        }

        if (action.startsWith(TOGGLE_EMAIL_STATUS)) {
            return handleToggleEmailStatus(action, roomId);
        }

        // 處理靜態動作
        return switch (action) {
            case EMAIL_MENU -> {
                logger.debug("Showing email menu for room: {}", roomId);
                yield emailFacade.showMenu(roomId);
            }

            case ADD_EMAIL -> {
                logger.info("Starting email input for room: {}", roomId);
                yield emailFacade.startAddingEmail(roomId);
            }

            case CANCEL_EMAIL_INPUT -> {
                logger.info("Cancelling email input for room: {}", roomId);
                yield emailFacade.cancelAddingEmail(roomId);
            }

            default -> {
                logger.warn("Unexpected action in EmailPostbackHandler: {}", action);
                yield null;
            }
        };
    }

    private Message handleDeleteEmail(String data, String roomId) {
        try {
            String idStr = data.substring(data.indexOf("&id=") + 4);
            Long emailId = Long.parseLong(idStr);
            return emailFacade.deleteEmail(emailId, roomId);
        } catch (Exception e) {
            logger.error("Delete email error: {}", e.getMessage(), e);
            return emailFacade.deleteEmail(null, roomId);
        }
    }

    private Message handleToggleEmailStatus(String data, String roomId) {
        try {
            String idStr = data.substring(data.indexOf("&id=") + 4);
            Long emailId = Long.parseLong(idStr);
            return emailFacade.toggleEmailStatus(emailId, roomId);
        } catch (Exception e) {
            logger.error("Toggle email status error: {}", e.getMessage(), e);
            return emailFacade.toggleEmailStatus(null, roomId);
        }
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
