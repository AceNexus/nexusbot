package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;

/**
 * 處理導航相關的 Postback 事件
 */
@Component
@RequiredArgsConstructor
public class NavigationPostbackEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(NavigationPostbackEventHandler.class);

    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.POSTBACK && event.hasAction(MAIN_MENU, HELP_MENU, ABOUT);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String action = event.getPayloadString("action");
        String roomId = event.getRoomId();

        logger.info("NavigationPostbackEventHandler handling action: {} for room: {}", action, roomId);

        return switch (action) {
            case MAIN_MENU -> messageTemplateProvider.mainMenu();
            case HELP_MENU -> messageTemplateProvider.helpMenu();
            case ABOUT -> messageTemplateProvider.about();
            default -> null;
        };
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
