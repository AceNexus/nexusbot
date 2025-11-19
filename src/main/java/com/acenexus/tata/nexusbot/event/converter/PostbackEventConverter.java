package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 將 LINE postback 事件轉換為統一的 LineBotEvent
 */
@Component
public class PostbackEventConverter implements EventConverter {

    @Override
    public boolean canConvert(JsonNode event) {
        return "postback".equals(event.path("type").asText());
    }

    @Override
    public LineBotEvent convert(JsonNode event) {
        JsonNode source = event.path("source");
        JsonNode postback = event.path("postback");

        return LineBotEvent.builder()
                .roomId(SourceExtractor.extractRoomId(source))
                .roomType(SourceExtractor.extractRoomType(source))
                .userId(SourceExtractor.extractUserId(source))
                .eventType(EventType.POSTBACK)
                .replyToken(event.path("replyToken").asText())
                .payload(extractPayload(postback))
                .rawEvent(event)
                .build();
    }

    private Map<String, Object> extractPayload(JsonNode postback) {
        Map<String, Object> payload = new HashMap<>();

        // 提取 action（來自 postback.data）
        String data = postback.path("data").asText();
        payload.put("action", data);

        // 提取 params（如果有 datetime 或其他參數）
        if (postback.has("params")) {
            Map<String, String> params = new HashMap<>();
            JsonNode paramsNode = postback.path("params");
            paramsNode.fieldNames().forEachRemaining(fieldName -> params.put(fieldName, paramsNode.path(fieldName).asText()));
            payload.put("params", params);
        }

        return payload;
    }
}
