package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.event.RoomType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Follow 事件轉換器
 * 處理使用者加入好友（follow）和封鎖機器人（unfollow）事件
 */
@Component
public class FollowEventConverter implements EventConverter {

    @Override
    public boolean canConvert(JsonNode event) {
        String type = event.path("type").asText();
        return "follow".equals(type) || "unfollow".equals(type);
    }

    @Override
    public LineBotEvent convert(JsonNode event) {
        String eventType = event.path("type").asText();
        JsonNode source = event.path("source");
        String userId = source.path("userId").asText();

        return LineBotEvent.builder()
                .roomId(userId)  // follow/unfollow 只發生在一對一聊天
                .roomType(RoomType.USER)
                .userId(userId)
                .eventType("follow".equals(eventType) ? EventType.FOLLOW : EventType.UNFOLLOW)
                .replyToken(event.path("replyToken").asText())
                .payload(Collections.emptyMap())
                .rawEvent(event)
                .build();
    }
}
