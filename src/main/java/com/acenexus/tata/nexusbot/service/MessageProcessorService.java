package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.constants.BotMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            default -> handleDefaultMessage(messageText);
        };

        log.debug("Text message processed from user {}: {}", userId, normalizedText);
        messageService.sendReply(replyToken, response);
    }

    private String handleDefaultMessage(String messageText) {
        String aiResponse = groqService.chat(messageText);
        if (aiResponse != null && !aiResponse.trim().isEmpty()) {
            return aiResponse;
        }
        return BotMessages.getDefaultTextResponse(messageText);
    }

    public void processImageMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getImageResponse(messageId);
        log.debug("Image message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processStickerMessage(String userId, String packageId, String stickerId, String replyToken) {
        String response = BotMessages.getStickerResponse(packageId, stickerId);
        log.debug("Sticker message processed from user {}: packageId={}, stickerId={}", userId, packageId, stickerId);
        messageService.sendReply(replyToken, response);
    }

    public void processVideoMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getVideoResponse(messageId);
        log.debug("Video message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processAudioMessage(String userId, String messageId, String replyToken) {
        String response = BotMessages.getAudioResponse(messageId);
        log.debug("Audio message processed from user {}: messageId={}", userId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processFileMessage(String userId, String messageId, String fileName, long fileSize, String replyToken) {
        String response = BotMessages.getFileResponse(fileName, fileSize);
        log.debug("File message processed from user {}: fileName={}, size={}, messageId={}", userId, fileName, fileSize, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processLocationMessage(String userId, String title, String address, double latitude, double longitude, String replyToken) {
        String response = BotMessages.getLocationResponse(title, address, latitude, longitude);
        log.debug("Location message processed from user {}: title={}, address={}, lat={}, lon={}", userId, title, address, latitude, longitude);
        messageService.sendReply(replyToken, response);
    }

    public void processDefaultMessage(String userId, String replyToken) {
        String response = BotMessages.UNKNOWN_MESSAGE_TYPE;
        log.warn("Default message handler used for user {}", userId);
        messageService.sendReply(replyToken, response);
    }
}
