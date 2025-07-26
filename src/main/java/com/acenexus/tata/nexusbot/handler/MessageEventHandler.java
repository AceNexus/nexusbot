package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.service.MessageProcessorService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageEventHandler.class);
    private final MessageProcessorService messageProcessorService;

    public void handle(JsonNode event) {
        try {
            JsonNode message = event.get("message");
            String messageType = message.get("type").asText();
            String replyToken = event.get("replyToken").asText();
            String userId = event.get("source").get("userId").asText();

            switch (messageType) {
                case "text" -> {
                    String userText = message.get("text").asText();
                    logger.info("User {} sent text message: {}", userId, userText);
                    messageProcessorService.processTextMessage(userId, userText, replyToken);
                }
                case "image" -> {
                    String messageId = message.get("id").asText();
                    logger.info("User {} sent image", userId);
                    messageProcessorService.processImageMessage(userId, messageId, replyToken);
                }
                case "sticker" -> {
                    String packageId = message.get("packageId").asText();
                    String stickerId = message.get("stickerId").asText();
                    logger.info("User {} sent sticker", userId);
                    messageProcessorService.processStickerMessage(userId, packageId, stickerId, replyToken);
                }
                case "video" -> {
                    String messageId = message.get("id").asText();
                    logger.info("User {} sent video", userId);
                    messageProcessorService.processVideoMessage(userId, messageId, replyToken);
                }
                case "audio" -> {
                    String messageId = message.get("id").asText();
                    logger.info("User {} sent audio", userId);
                    messageProcessorService.processAudioMessage(userId, messageId, replyToken);
                }
                case "file" -> {
                    String messageId = message.get("id").asText();
                    String fileName = message.get("fileName").asText();
                    long fileSize = message.get("fileSize").asLong();
                    logger.info("User {} sent file: {}", userId, fileName);
                    messageProcessorService.processFileMessage(userId, messageId, fileName, fileSize, replyToken);
                }
                case "location" -> {
                    String title = message.has("title") ? message.get("title").asText() : null;
                    String address = message.has("address") ? message.get("address").asText() : null;
                    double latitude = message.get("latitude").asDouble();
                    double longitude = message.get("longitude").asDouble();
                    logger.info("User {} sent location", userId);
                    messageProcessorService.processLocationMessage(userId, title, address, latitude, longitude, replyToken);
                }
                default -> {
                    logger.info("User {} sent unhandled message type: {}", userId, messageType);
                    messageProcessorService.processDefaultMessage(userId, replyToken);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message event: {}", e.getMessage(), e);
        }
    }
}