package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;

/**
 * 導航功能 Handler - 處理主選單、說明、關於頁面
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class NavigationPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(NavigationPostbackHandler.class);

    private final MessageTemplateProvider messageTemplateProvider;
    private final ChatRoomManager chatRoomManager;

    @Override
    public boolean canHandle(String action) {
        return switch (action) {
            case MAIN_MENU, HELP_MENU, ABOUT -> true;
            default -> false;
        };
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("NavigationPostbackHandler handling action: {} for room: {}", action, roomId);

        return switch (action) {
            case MAIN_MENU -> {
                logger.debug("Showing main menu for room: {}", roomId);
                yield messageTemplateProvider.mainMenu();
            }
            case HELP_MENU -> {
                logger.debug("Showing help menu for room: {}", roomId);
                yield messageTemplateProvider.helpMenu();
            }
            case ABOUT -> {
                logger.debug("Showing about page for room: {}", roomId);
                yield messageTemplateProvider.about();
            }
            default -> {
                logger.warn("Unexpected action in NavigationPostbackHandler: {}", action);
                yield null;
            }
        };
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
