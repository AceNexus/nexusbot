package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 處理台股分析命令
 */
@Component
@RequiredArgsConstructor
public class StockCommandEventHandler implements LineBotEventHandler {

    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        String normalizedText = event.getNormalizedText();
        return normalizedText != null && normalizedText.equals("stock");
    }

    @Override
    public Message handle(LineBotEvent event) {
        return messageTemplateProvider.stockAnalysisMenu();
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
