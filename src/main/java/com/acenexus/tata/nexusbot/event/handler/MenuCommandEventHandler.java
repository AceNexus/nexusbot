package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 處理選單命令
 */
@Component
@RequiredArgsConstructor
public class MenuCommandEventHandler implements LineBotEventHandler {

    private final MessageTemplateProvider messageTemplateProvider;

    private static final Set<String> MENU_COMMANDS = Set.of("menu", "選單");

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        String text = event.getPayloadString("text");
        if (text == null) {
            return false;
        }

        String normalizedText = text.toLowerCase().trim();
        return MENU_COMMANDS.contains(normalizedText);
    }

    @Override
    public Message handle(LineBotEvent event) {
        return messageTemplateProvider.mainMenu();
    }

    @Override
    public int getPriority() {
        return 50; // 較低優先級，在其他處理器之後
    }
}
