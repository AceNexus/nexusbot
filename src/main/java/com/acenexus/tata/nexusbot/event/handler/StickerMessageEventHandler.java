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
 * 處理貼圖訊息事件
 */
@Component
@RequiredArgsConstructor
public class StickerMessageEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(StickerMessageEventHandler.class);

    private final MessageProcessorService messageProcessorService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.STICKER_MESSAGE;
    }

    @Override
    public Message handle(LineBotEvent event) {
        String packageId = event.getPayloadString("packageId");
        String stickerId = event.getPayloadString("stickerId");
        logger.info("Room {} sent sticker", event.getRoomId());
        messageProcessorService.processStickerMessage(event.getRoomId(), packageId, stickerId, event.getReplyToken());
        return null;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
