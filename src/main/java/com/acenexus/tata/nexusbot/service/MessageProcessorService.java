package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandDispatcher;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.entity.ChatMessage;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.facade.LocationFacade;
import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 消息處理服務
 * 職責：
 * 1. 消息類型分發
 * 2. AI 對話處理
 * 3. 協調命令處理和業務邏輯
 */
@Service
@RequiredArgsConstructor
public class MessageProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessorService.class);

    private final MessageService messageService;
    private final AIService aiService;
    private final MessageTemplateProvider messageTemplateProvider;
    private final ChatRoomManager chatRoomManager;
    private final ChatMessageRepository chatMessageRepository;
    private final LocationFacade locationFacade;

    // 命令分發器
    private final CommandDispatcher commandDispatcher;

    public void processTextMessage(String roomId, String sourceType, String userId, String messageText, String replyToken) {
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

        // 使用命令分發器處理所有命令
        CommandResult result = commandDispatcher.dispatch(context);

        if (result.isHandled()) {
            if (result.getMessage() != null) {
                messageService.sendMessage(replyToken, result.getMessage());
            } else if (result.getTextResponse() != null) {
                messageService.sendReply(replyToken, result.getTextResponse());
            }
            return;
        }

        // 檢查 AI 是否啟用
        if (!chatRoomManager.isAiEnabled(roomId, roomType)) {
            logger.info("AI disabled for room: {} (type: {}), skipping AI processing", roomId, roomType);
            return;
        }

        // 儲存用戶對話
        ChatMessage userMessage = ChatMessage.createUserMessage(roomId, roomType, userId, messageText);
        chatMessageRepository.save(userMessage);

        // 非同步處理 AI 對話
        handleAIMessage(roomId, roomType, messageText, replyToken);
    }

    private void handleAIMessage(String roomId, ChatRoom.RoomType roomType, String messageText, String replyToken) {
        CompletableFuture.runAsync(() -> {
            try {
                // 獲取聊天室指定的AI模型
                String selectedModel = chatRoomManager.getAiModel(roomId, roomType);
                AIService.ChatResponse chatResponse = aiService.chatWithContext(roomId, messageText, selectedModel);
                String finalResponse = (chatResponse.success() && chatResponse.content() != null && !chatResponse.content().trim().isEmpty()) ? chatResponse.content() : messageTemplateProvider.defaultTextResponse(messageText);

                messageService.sendReply(replyToken, finalResponse);

                // 儲存 AI 對話（現在有真實的 tokens 和處理時間）
                ChatMessage aiMessage = ChatMessage.createAIMessage(roomId, roomType, finalResponse, chatResponse.model(), chatResponse.tokensUsed(), chatResponse.processingTime().intValue());
                chatMessageRepository.save(aiMessage);

                logger.info("AI response sent to room {}, tokens: {}, time: {}ms", roomId, chatResponse.tokensUsed(), chatResponse.processingTime());
            } catch (Exception e) {
                logger.error("AI processing error for room {}: {}", roomId, e.getMessage());
                String fallbackResponse = messageTemplateProvider.defaultTextResponse(messageText);
                messageService.sendReply(replyToken, fallbackResponse);

                // 也儲存錯誤回應
                ChatMessage aiMessage = ChatMessage.createAIMessage(roomId, roomType, fallbackResponse, "fallback", 0, 0);
                chatMessageRepository.save(aiMessage);
            }
        });
    }

    public void processImageMessage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.imageResponse(messageId);
        logger.info("Image message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processStickerMessage(String roomId, String packageId, String stickerId, String replyToken) {
        String response = messageTemplateProvider.stickerResponse(packageId, stickerId);
        logger.info("Sticker message processed from room {}: packageId={}, stickerId={}", roomId, packageId, stickerId);
        messageService.sendReply(replyToken, response);
    }

    public void processVideoMessage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.videoResponse(messageId);
        logger.info("Video message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processAudioMessage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.audioResponse(messageId);
        logger.info("Audio message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processFileMessage(String roomId, String messageId, String fileName, long fileSize, String replyToken) {
        String response = messageTemplateProvider.fileResponse(fileName, fileSize);
        logger.info("File message processed from room {}: fileName={}, size={}, messageId={}", roomId, fileName, fileSize, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processLocationMessage(String roomId, String title, String address, double latitude, double longitude, String replyToken) {
        Message response = locationFacade.handleLocationMessage(roomId, title, address, latitude, longitude, replyToken);
        if (response != null) {
            messageService.sendMessage(replyToken, response);
        }
    }

    public void processDefaultMessage(String roomId, String replyToken) {
        String response = messageTemplateProvider.unknownMessage();
        logger.warn("Default message handler used for room {}", roomId);
        messageService.sendReply(replyToken, response);
    }
}
