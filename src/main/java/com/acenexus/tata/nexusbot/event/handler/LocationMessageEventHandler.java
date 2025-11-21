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
 * 處理位置訊息事件
 */
@Component
@RequiredArgsConstructor
public class LocationMessageEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(LocationMessageEventHandler.class);

    private final MessageProcessorService messageProcessorService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.LOCATION_MESSAGE;
    }

    @Override
    public Message handle(LineBotEvent event) {
        String title = event.getPayloadString("title");
        String address = event.getPayloadString("address");
        Double latitude = event.getPayloadDouble("latitude");
        Double longitude = event.getPayloadDouble("longitude");
        logger.info("Room {} sent location", event.getRoomId());
        messageProcessorService.processLocationMessage(event.getRoomId(), title, address, latitude != null ? latitude : 0.0, longitude != null ? longitude : 0.0, event.getReplyToken());
        return null;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
