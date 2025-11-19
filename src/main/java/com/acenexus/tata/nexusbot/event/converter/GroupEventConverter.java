package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.event.RoomType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * 群組事件轉換器
 * 處理群組相關事件（join, leave, memberJoined, memberLeft）
 */
@Component
public class GroupEventConverter implements EventConverter {

    private static final Set<String> GROUP_EVENT_TYPES = Set.of("join", "leave", "memberJoined", "memberLeft");

    @Override
    public boolean canConvert(JsonNode event) {
        String type = event.path("type").asText();
        return GROUP_EVENT_TYPES.contains(type);
    }

    @Override
    public LineBotEvent convert(JsonNode event) {
        String eventTypeStr = event.path("type").asText();
        JsonNode source = event.path("source");
        String groupId = source.path("groupId").asText();
        String userId = source.path("userId").asText();

        EventType eventType = mapEventType(eventTypeStr);
        Map<String, Object> payload = extractPayload(event, eventTypeStr);

        return LineBotEvent.builder()
                .roomId(groupId)
                .roomType(RoomType.GROUP)
                .userId(userId)
                .eventType(eventType)
                .replyToken(event.path("replyToken").asText())
                .payload(payload)
                .rawEvent(event)
                .build();
    }

    private EventType mapEventType(String eventTypeStr) {
        return switch (eventTypeStr) {
            case "join" -> EventType.JOIN;
            case "leave" -> EventType.LEAVE;
            case "memberJoined" -> EventType.MEMBER_JOINED;
            case "memberLeft" -> EventType.MEMBER_LEFT;
            default -> EventType.UNKNOWN;
        };
    }

    private Map<String, Object> extractPayload(JsonNode event, String eventTypeStr) {
        Map<String, Object> payload = new HashMap<>();

        // memberJoined 和 memberLeft 需要提取加入/離開的成員清單
        if ("memberJoined".equals(eventTypeStr) && event.has("joined")) {
            JsonNode joinedNode = event.path("joined").path("members");
            List<String> joinedUsers = extractUserIds(joinedNode);
            payload.put("joinedUsers", joinedUsers);
        } else if ("memberLeft".equals(eventTypeStr) && event.has("left")) {
            JsonNode leftNode = event.path("left").path("members");
            List<String> leftUsers = extractUserIds(leftNode);
            payload.put("leftUsers", leftUsers);
        }

        return payload;
    }

    private List<String> extractUserIds(JsonNode membersNode) {
        if (membersNode.isMissingNode() || !membersNode.isArray()) {
            return Collections.emptyList();
        }

        return StreamSupport.stream(membersNode.spliterator(), false)
                .map(member -> member.path("userId").asText())
                .filter(userId -> !userId.isEmpty())
                .toList();
    }
}
