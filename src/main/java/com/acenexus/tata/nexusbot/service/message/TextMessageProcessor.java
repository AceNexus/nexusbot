package com.acenexus.tata.nexusbot.service.message;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandDispatcher;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.service.ai.AIMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文字消息處理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextMessageProcessor {

    private final MessageService messageService;
    private final ChatRoomManager chatRoomManager;
    private final CommandDispatcher commandDispatcher;
    private final AIMessageHandler aiMessageHandler;

    /**
     * 處理文字消息
     *
     * @param roomId      聊天室 ID
     * @param sourceType  來源類型
     * @param userId      用戶 ID
     * @param messageText 消息文字
     * @param replyToken  回覆 Token
     */
    public void process(String roomId, String sourceType, String userId, String messageText, String replyToken) {
        String normalizedText = messageText.toLowerCase().trim();
        ChatRoom.RoomType roomType = chatRoomManager.determineRoomType(sourceType);

        // 構建命令上下文
        CommandContext context = CommandContext.builder()
                .roomId(roomId)
                .roomType(roomType)
                .userId(userId)
                .messageText(messageText)
                .normalizedText(normalizedText)
                .replyToken(replyToken)
                .build();

        // 命令處理
        CommandResult result = commandDispatcher.dispatch(context);

        if (result.isHandled()) {
            sendResponse(result, replyToken);
            return;
        }

        // AI 處理
        if (!chatRoomManager.isAiEnabled(roomId, roomType)) {
            log.info("AI disabled for room: {}, skipping AI processing", roomId);
            return;
        }

        aiMessageHandler.handleAIMessage(roomId, roomType, userId, messageText, replyToken);
    }

    /**
     * 發送命令處理結果
     */
    private void sendResponse(CommandResult result, String replyToken) {
        if (result.getMessage() != null) {
            messageService.sendMessage(replyToken, result.getMessage());
        } else if (result.getTextResponse() != null) {
            messageService.sendReply(replyToken, result.getTextResponse());
        }
    }
}
