package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.service.AdminService;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 處理認證命令和密碼輸入
 */
@Component
@RequiredArgsConstructor
public class AuthCommandEventHandler implements LineBotEventHandler {

    private final AdminService adminService;
    private final MessageService messageService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        String text = event.getPayloadString("text");
        if (text == null) {
            return false;
        }

        String normalizedText = text.toLowerCase().trim();
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        // 處理 /auth 命令或正在等待密碼輸入的聊天室
        return normalizedText.equals("/auth") || adminService.isAuthPending(event.getRoomId(), roomType);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        String response = adminService.processAuthCommand(event.getRoomId(), roomType, text);

        if (response != null) {
            messageService.sendReply(event.getReplyToken(), response);
        }

        return null; // 已透過 MessageService 發送
    }

    @Override
    public int getPriority() {
        return 1; // 最高優先級
    }
}
