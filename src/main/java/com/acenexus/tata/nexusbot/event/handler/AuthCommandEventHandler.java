package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.admin.AdminService;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 處理認證命令和密碼輸入
 */
@Component
@RequiredArgsConstructor
public class AuthCommandEventHandler implements LineBotEventHandler {

    private final AdminService adminService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        String normalizedText = event.getNormalizedText();
        if (normalizedText == null) {
            return false;
        }

        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        // 處理 /auth 命令或正在等待密碼輸入的聊天室
        return normalizedText.equals("/auth") || adminService.isAuthPending(event.getRoomId(), roomType);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        String response = adminService.processAuthCommand(event.getRoomId(), roomType, text);

        if (response == null) {
            return null;
        }
        return new TextMessage(response);
    }

    @Override
    public int getPriority() {
        return 1; // 最高優先級
    }
}
