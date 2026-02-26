package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.util.TimezoneValidator;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 時區設定指令處理器
 * 支援：
 * - tz Asia/Tokyo
 * - timezone America/New_York
 * - 時區 東京
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimezoneCommandEventHandler implements LineBotEventHandler {

    private final ChatRoomManager chatRoomManager;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }
        String text = event.getPayloadString("text");
        if (text == null) return false;
        String normalized = text.trim().toLowerCase();
        return normalized.startsWith("tz ") || normalized.startsWith("timezone ") || text.startsWith("時區");
    }

    @Override
    public Message handle(LineBotEvent event) {
        String rawText = event.getPayloadString("text");
        String normalized = rawText.trim().toLowerCase();

        String prefix;
        if (normalized.startsWith("tz ")) {
            prefix = "tz";
        } else if (normalized.startsWith("timezone ")) {
            prefix = "timezone";
        } else if (rawText.startsWith("時區")) {
            prefix = "時區";
        } else {
            return null;
        }

        String input = rawText.substring(prefix.length()).trim();
        if (input.isEmpty()) {
            return new TextMessage("請提供要設定的時區，例如：tz Asia/Tokyo 或 時區 台北");
        }

        String resolved = TimezoneValidator.resolveTimezone(input);
        if (resolved == null) {
            return new TextMessage("無法辨識時區：「" + input + "」，請使用 IANA 時區（如 Asia/Taipei）或常用別名（台北、東京、紐約）。");
        }

        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());
        boolean success = chatRoomManager.setTimezone(event.getRoomId(), roomType, resolved);

        if (success) {
            String display = TimezoneValidator.getDisplayName(resolved);
            return new TextMessage("已更新聊天室時區為：" + display + "。之後的提醒將以此時區解析。");
        } else {
            return new TextMessage("更新時區失敗，請稍後再試。");
        }
    }

    @Override
    public int getPriority() {
        return 2; // 在提醒流程之前處理
    }
}







