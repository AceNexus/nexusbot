package com.acenexus.tata.nexusbot.command;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

/**
 * 命令執行上下文（包含所有處理所需信息）
 */
@Getter
@Builder
public class CommandContext {
    private final String roomId;
    private final ChatRoom.RoomType roomType;
    private final String userId;
    private final String messageText;
    private final String normalizedText;
    private final String replyToken;
}
