package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.constants.BotMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final MessageService messageService;
    private final GroqService groqService;

    public void processTextMessage(String userId, String messageText, String replyToken) {
        String normalizedText = messageText.toLowerCase().trim();

        String response = switch (normalizedText) {
            case "menu", "選單" -> BotMessages.getMenuMessage();
            default -> {
                // 只有 AI 調用非同步處理
                CompletableFuture.runAsync(() -> {
                    try {
                        String aiResponse = groqService.chat(messageText);
                        String finalResponse = (aiResponse != null && !aiResponse.trim().isEmpty()) ? aiResponse : BotMessages.getDefaultTextResponse(messageText);
                        messageService.sendReply(replyToken, finalResponse);
                        log.info("AI response sent to user {}", userId);
                    } catch (Exception e) {
                        log.error("AI processing error for user {}: {}", userId, e.getMessage());
                        messageService.sendReply(replyToken, BotMessages.getDefaultTextResponse(messageText));
                    }
                });
                yield null; // 不立即回應
            }
        };

        if (response != null) {
            log.info("Quick response for user {}: {}", userId, normalizedText);
            messageService.sendReply(replyToken, response);
        }
    }


    public void processImageMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getImageResponse(messageId);
        log.info("Image message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processStickerMessage(String userId, String packageId, String stickerId, String replyToken) {
        String response = BotMessages.getStickerResponse(packageId, stickerId);
        log.info("Sticker message processed from user {}: packageId={}, stickerId={}", userId, packageId, stickerId);
        messageService.sendReply(replyToken, response);
    }

    public void processVideoMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getVideoResponse(messageId);
        log.info("Video message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processAudioMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getAudioResponse(messageId);
        log.info("Audio message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processFileMessage(String userId, String messageId, String fileName, long fileSize, String replyToken) {
        String response = BotMessages.getFileResponse(fileName, fileSize);
        log.info("File message processed from user {}: fileName={}, size={}, messageId={}", userId, fileName, fileSize, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processLocationMessage(String userId, String title, String address, double latitude, double longitude, String replyToken) {
        String response = BotMessages.getLocationResponse(title, address, latitude, longitude);
        log.info("Location message processed from user {}: title={}, address={}, lat={}, lon={}", userId, title, address, latitude, longitude);
        messageService.sendReply(replyToken, response);
    }

    public void processDefaultMessage(String userId, String replyToken) {
        String response = BotMessages.UNKNOWN_MESSAGE_TYPE;
        log.warn("Default message handler used for user {}", userId);
        messageService.sendReply(replyToken, response);
    }
}
