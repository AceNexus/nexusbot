package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MessageProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessorService.class);
    private final MessageService messageService;
    private final AIService aiService;
    private final MessageTemplateProvider messageTemplateProvider;
    private final ChatRoomManager chatRoomManager;

    public void processTextMessage(String roomId, String sourceType, String messageText, String replyToken) {
        String normalizedText = messageText.toLowerCase().trim();
        ChatRoom.RoomType roomType = chatRoomManager.determineRoomType(sourceType);

        // 處理預定義指令
        if (handlePredefinedCommand(normalizedText, roomId, replyToken)) {
            return;
        }

        // 檢查 AI 是否啟用
        if (!chatRoomManager.isAiEnabled(roomId, roomType)) {
            logger.info("AI disabled for room: {} (type: {}), skipping AI processing", roomId, roomType);
            return;
        }

        // 非同步處理 AI 對話
        handleAIMessage(roomId, messageText, replyToken);
    }

    private boolean handlePredefinedCommand(String normalizedText, String roomId, String replyToken) {
        try {
            switch (normalizedText) {
                case "menu", "選單" -> {
                    messageService.sendMessage(replyToken, messageTemplateProvider.mainMenu());
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error sending predefined response to room {}: {}", roomId, e.getMessage());
            return false; // 讓它繼續走 AI 處理
        }
    }

    private void handleAIMessage(String roomId, String messageText, String replyToken) {
        CompletableFuture.runAsync(() -> {
            try {
                String aiResponse = aiService.chat(messageText);
                String finalResponse = (aiResponse != null && !aiResponse.trim().isEmpty()) ?
                        aiResponse : messageTemplateProvider.defaultTextResponse(messageText);
                messageService.sendReply(replyToken, finalResponse);
                logger.info("AI response sent to room {}", roomId);
            } catch (Exception e) {
                logger.error("AI processing error for room {}: {}", roomId, e.getMessage());
                messageService.sendReply(replyToken, messageTemplateProvider.defaultTextResponse(messageText));
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
        String response = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
        logger.info("Location message processed from room {}: title={}, address={}, lat={}, lon={}", roomId, title, address, latitude, longitude);
        messageService.sendReply(replyToken, response);
    }

    public void processDefaultMessage(String roomId, String replyToken) {
        String response = messageTemplateProvider.unknownMessage();
        logger.warn("Default message handler used for room {}", roomId);
        messageService.sendReply(replyToken, response);
    }
}
