package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 將 LINE message 事件轉換為統一的 LineBotEvent
 */
@Component
public class MessageEventConverter implements EventConverter {
    private static final Logger logger = LoggerFactory.getLogger(MessageEventConverter.class);

    @Override
    public boolean canConvert(JsonNode event) {
        return "message".equals(event.path("type").asText());
    }

    @Override
    public LineBotEvent convert(JsonNode event) {
        JsonNode source = event.path("source");
        JsonNode message = event.path("message");

        String messageType = message.path("type").asText();
        EventType eventType = mapMessageType(messageType);

        return LineBotEvent.builder()
                .roomId(SourceExtractor.extractRoomId(source))
                .roomType(SourceExtractor.extractRoomType(source))
                .userId(SourceExtractor.extractUserId(source))
                .eventType(eventType)
                .replyToken(event.path("replyToken").asText())
                .payload(extractPayload(message, messageType))
                .rawEvent(event)
                .build();
    }

    private EventType mapMessageType(String messageType) {
        return switch (messageType) {
            case "text" -> EventType.TEXT_MESSAGE;
            case "image" -> EventType.IMAGE_MESSAGE;
            case "sticker" -> EventType.STICKER_MESSAGE;
            case "video" -> EventType.VIDEO_MESSAGE;
            case "audio" -> EventType.AUDIO_MESSAGE;
            case "file" -> EventType.FILE_MESSAGE;
            case "location" -> EventType.LOCATION_MESSAGE;
            default -> {
                logger.warn("Unknown message type: {}", messageType);
                yield EventType.UNKNOWN;
            }
        };
    }

    private Map<String, Object> extractPayload(JsonNode message, String messageType) {
        Map<String, Object> payload = new HashMap<>();

        switch (messageType) {
            case "text" -> payload.put("text", message.path("text").asText());
            case "image", "video", "audio" -> payload.put("messageId", message.path("id").asText());
            case "sticker" -> {
                payload.put("packageId", message.path("packageId").asText());
                payload.put("stickerId", message.path("stickerId").asText());
            }
            case "file" -> {
                payload.put("messageId", message.path("id").asText());
                payload.put("fileName", message.path("fileName").asText());
                payload.put("fileSize", message.path("fileSize").asLong());
            }
            case "location" -> {
                payload.put("title", message.path("title").asText());
                payload.put("address", message.path("address").asText());
                payload.put("latitude", message.path("latitude").asDouble());
                payload.put("longitude", message.path("longitude").asDouble());
            }
        }

        return payload;
    }
}
