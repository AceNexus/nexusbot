package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.service.MessageProcessorService;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理文字訊息事件
 */
@Component
@RequiredArgsConstructor
public class TextMessageEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(TextMessageEventHandler.class);

    private final MessageProcessorService messageProcessorService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.TEXT_MESSAGE;
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        logger.info("Room {} sent text message: {}", event.getRoomId(), text);
        messageProcessorService.processTextMessage(event.getRoomId(), event.getRoomType().name(), event.getUserId(), text, event.getReplyToken());
        return null;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
