package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.ai.AIMessageHandler;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 處理 AI 對話（未被其他命令處理的文字消息）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIMessageEventHandler implements LineBotEventHandler {

    private final ChatRoomManager chatRoomManager;
    private final AIMessageHandler aiMessageHandler;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        // 只處理啟用 AI 的聊天室
        return chatRoomManager.isAiEnabled(event.getRoomId(), roomType);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());
        log.info("AI processing message from room: {}", event.getRoomId());
        aiMessageHandler.handleAIMessage(event.getRoomId(), roomType, event.getUserId(), text, event.getReplyToken());
        return null; // AI 處理器內部會發送訊息
    }

    @Override
    public int getPriority() {
        return 100; // 最低優先級，處理所有未被命令處理的文字消息
    }
}
