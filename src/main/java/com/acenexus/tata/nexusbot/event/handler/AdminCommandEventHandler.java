package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.admin.AdminService;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 處理管理員命令
 */
@Component
@RequiredArgsConstructor
public class AdminCommandEventHandler implements LineBotEventHandler {

    private final AdminService adminService;

    private static final Set<String> ADMIN_COMMANDS = Set.of("/stats", "/health", "/info");

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

        // 必須是管理員且命令在列表中
        return adminService.isAdminRoom(event.getRoomId(), roomType) && ADMIN_COMMANDS.contains(normalizedText);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        return adminService.processAdminCommand(event.getRoomId(), roomType, text);
    }

    @Override
    public int getPriority() {
        return 2; // 高優先級，僅次於認證
    }
}
