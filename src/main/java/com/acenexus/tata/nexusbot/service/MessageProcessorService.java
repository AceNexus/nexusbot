package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.constants.BotMessages;
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
    private final GroqService groqService;
    private final FlexMenuService flexMenuService;

    public void processTextMessage(String userId, String messageText, String replyToken) {
        String normalizedText = messageText.toLowerCase().trim();

        // 處理預定義指令
        if (handlePredefinedCommand(normalizedText, userId, replyToken)) {
            return;
        }

        // 2. 非同步處理 AI 對話
        handleAIMessage(userId, messageText, replyToken);
    }

    private boolean handlePredefinedCommand(String normalizedText, String userId, String replyToken) {
        try {
            switch (normalizedText) {
                case "menu", "選單" -> {
                    messageService.sendMessage(replyToken, flexMenuService.createMenuFlexMessage());
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error sending predefined response to user {}: {}", userId, e.getMessage());
            return false; // 讓它繼續走 AI 處理
        }
    }

    private void handleAIMessage(String userId, String messageText, String replyToken) {
        CompletableFuture.runAsync(() -> {
            try {
                String aiResponse = groqService.chat(messageText);
                String finalResponse = (aiResponse != null && !aiResponse.trim().isEmpty()) ? aiResponse : BotMessages.getDefaultTextResponse(messageText);
                messageService.sendReply(replyToken, finalResponse);
                logger.info("AI response sent to user {}", userId);
            } catch (Exception e) {
                logger.error("AI processing error for user {}: {}", userId, e.getMessage());
                messageService.sendReply(replyToken, BotMessages.getDefaultTextResponse(messageText));
            }
        });
    }

    public void processImageMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getImageResponse(messageId);
        logger.info("Image message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processStickerMessage(String userId, String packageId, String stickerId, String replyToken) {
        String response = BotMessages.getStickerResponse(packageId, stickerId);
        logger.info("Sticker message processed from user {}: packageId={}, stickerId={}", userId, packageId, stickerId);
        messageService.sendReply(replyToken, response);
    }

    public void processVideoMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getVideoResponse(messageId);
        logger.info("Video message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processAudioMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getAudioResponse(messageId);
        logger.info("Audio message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processFileMessage(String userId, String messageId, String fileName, long fileSize, String replyToken) {
        String response = BotMessages.getFileResponse(fileName, fileSize);
        logger.info("File message processed from user {}: fileName={}, size={}, messageId={}", userId, fileName, fileSize, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processLocationMessage(String userId, String title, String address, double latitude, double longitude, String replyToken) {
        String response = BotMessages.getLocationResponse(title, address, latitude, longitude);
        logger.info("Location message processed from user {}: title={}, address={}, lat={}, lon={}", userId, title, address, latitude, longitude);
        messageService.sendReply(replyToken, response);
    }

    public void processDefaultMessage(String userId, String replyToken) {
        String response = BotMessages.UNKNOWN_MESSAGE_TYPE;
        logger.warn("Default message handler used for user {}", userId);
        messageService.sendReply(replyToken, response);
    }
}
