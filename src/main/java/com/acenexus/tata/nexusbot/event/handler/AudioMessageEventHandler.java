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
 * 處理音訊訊息事件
 */
@Component
@RequiredArgsConstructor
public class AudioMessageEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(AudioMessageEventHandler.class);

    private final MessageProcessorService messageProcessorService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.AUDIO_MESSAGE;
    }

    @Override
    public Message handle(LineBotEvent event) {
        String messageId = event.getPayloadString("messageId");
        logger.info("Room {} sent audio", event.getRoomId());
        messageProcessorService.processAudioMessage(event.getRoomId(), messageId, event.getReplyToken());
        return null;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
