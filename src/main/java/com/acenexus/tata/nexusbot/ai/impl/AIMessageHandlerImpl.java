package com.acenexus.tata.nexusbot.ai.impl;

import com.acenexus.tata.nexusbot.ai.AIMessageHandler;
import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatMessage;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.acenexus.tata.nexusbot.util.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIMessageHandlerImpl implements AIMessageHandler {

    private final MessageService messageService;
    private final AIService aiService;
    private final ChatRoomManager chatRoomManager;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public void handleAIMessage(String roomId, ChatRoom.RoomType roomType, String userId, String messageText, String replyToken) {
        // 儲存用戶消息
        ChatMessage userMessage = ChatMessage.createUserMessage(roomId, roomType, userId, messageText);
        chatMessageRepository.save(userMessage);

        // 非同步處理 AI 對話（使用 MdcTaskDecorator 自動傳遞 traceId）
        CompletableFuture.runAsync(
                MdcTaskDecorator.wrap(() -> processAIConversation(roomId, roomType, messageText, replyToken))
        );
    }

    /**
     * 處理 AI 對話
     */
    private void processAIConversation(String roomId, ChatRoom.RoomType roomType, String messageText, String replyToken) {
        try {
            String selectedModel = chatRoomManager.getAiModel(roomId, roomType);
            AIService.ChatResponse chatResponse = aiService.chatWithContext(roomId, messageText, selectedModel);

            String finalResponse = (chatResponse.success() && chatResponse.content() != null && !chatResponse.content().trim().isEmpty()) ? chatResponse.content() : messageTemplateProvider.defaultTextResponse(messageText);

            messageService.sendReply(replyToken, finalResponse);

            // 儲存 AI 回應
            ChatMessage aiMessage = ChatMessage.createAIMessage(roomId, roomType, finalResponse, chatResponse.model(), chatResponse.tokensUsed(), chatResponse.processingTime().intValue());
            chatMessageRepository.save(aiMessage);

            log.info("AI response sent to room {}, tokens: {}, time: {}ms", roomId, chatResponse.tokensUsed(), chatResponse.processingTime());

        } catch (Exception e) {
            log.error("AI processing error for room {}: {}", roomId, e.getMessage());
            handleAIError(roomId, roomType, messageText, replyToken);
        }
    }

    /**
     * 處理 AI 錯誤
     */
    private void handleAIError(String roomId, ChatRoom.RoomType roomType, String messageText, String replyToken) {
        String fallbackResponse = messageTemplateProvider.defaultTextResponse(messageText);
        messageService.sendReply(replyToken, fallbackResponse);

        ChatMessage aiMessage = ChatMessage.createAIMessage(roomId, roomType, fallbackResponse, "fallback", 0, 0);
        chatMessageRepository.save(aiMessage);
    }
}
