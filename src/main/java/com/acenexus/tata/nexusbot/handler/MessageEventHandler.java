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

            // 獲取來源資訊
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();
            String roomId = sourceType.equals("group") ? source.get("groupId").asText() : source.get("userId").asText();
            String userId = source.get("userId").asText();

            switch (messageType) {
                case "text" -> {
                    String userText = message.get("text").asText();
                    logger.info("Room {} (type: {}) sent text message: {}", roomId, sourceType, userText);
                    messageProcessorService.processTextMessage(roomId, sourceType, userId, userText, replyToken);
                }
                case "image" -> {
                    String messageId = message.get("id").asText();
                    logger.info("Room {} sent image", roomId);
                    messageProcessorService.processImageMessage(roomId, messageId, replyToken);
                }
                case "sticker" -> {
                    String packageId = message.get("packageId").asText();
                    String stickerId = message.get("stickerId").asText();
                    logger.info("Room {} sent sticker", roomId);
                    messageProcessorService.processStickerMessage(roomId, packageId, stickerId, replyToken);
                }
                case "video" -> {
                    String messageId = message.get("id").asText();
                    logger.info("Room {} sent video", roomId);
                    messageProcessorService.processVideoMessage(roomId, messageId, replyToken);
                }
                case "audio" -> {
                    String messageId = message.get("id").asText();
                    logger.info("Room {} sent audio", roomId);
                    messageProcessorService.processAudioMessage(roomId, messageId, replyToken);
                }
                case "file" -> {
                    String messageId = message.get("id").asText();
                    String fileName = message.get("fileName").asText();
                    long fileSize = message.get("fileSize").asLong();
                    logger.info("Room {} sent file: {}", roomId, fileName);
                    messageProcessorService.processFileMessage(roomId, messageId, fileName, fileSize, replyToken);
                }
                case "location" -> {
                    String title = message.has("title") ? message.get("title").asText() : null;
                    String address = message.has("address") ? message.get("address").asText() : null;
                    double latitude = message.get("latitude").asDouble();
                    double longitude = message.get("longitude").asDouble();
                    logger.info("Room {} sent location", roomId);
                    messageProcessorService.processLocationMessage(roomId, title, address, latitude, longitude, replyToken);
                }
                default -> {
                    logger.info("Room {} sent unhandled message type: {}", roomId, messageType);
                    messageProcessorService.processDefaultMessage(roomId, replyToken);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message event: {}", e.getMessage(), e);
        }
    }
}