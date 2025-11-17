package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.facade.NavigationFacade;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
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
 * 導航功能 Handler
 * 職責：純路由，將請求委派給 NavigationFacade
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class NavigationPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(NavigationPostbackHandler.class);

    private final NavigationFacade navigationFacade;

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
            case MAIN_MENU -> navigationFacade.showMainMenu();
            case HELP_MENU -> navigationFacade.showHelpMenu();
            case ABOUT -> navigationFacade.showAbout();
            default -> null;
        };
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
