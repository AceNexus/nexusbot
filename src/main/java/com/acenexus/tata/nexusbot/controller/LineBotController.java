package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.service.LineBotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LineBotController {

    private final ObjectMapper objectMapper;
    private final LineBotService lineBotService;

    @Value("${line.bot.channel-secret}")
    private String channelSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "X-Line-Signature", required = false) String signature) {

        try {
            if (signature != null && !validateSignature(payload, signature)) {
                log.warn("無效的 webhook 簽名");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            log.info("接收到 LINE webhook 請求，payload 長度: {} bytes", payload.length());

            JsonNode requestBody = objectMapper.readTree(payload);
            JsonNode events = requestBody.get("events");

            if (events != null && events.isArray() && !events.isEmpty()) {
                log.info("處理 {} 個事件", events.size());

                for (JsonNode event : events) {
                    try {
                        handleEvent(event);
                    } catch (Exception eventException) {
                        log.error("處理單一事件時發生錯誤: {}", eventException.getMessage(), eventException);
                    }
                }
            } else {
                log.info("收到空的事件陣列或無效的 payload");
            }

            return ResponseEntity.status(HttpStatus.OK).body("OK");

        } catch (Exception e) {
            log.error("Webhook 處理過程發生嚴重錯誤: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        }
    }

    private void handleEvent(JsonNode event) {
        String eventType = event.get("type").asText();
        switch (eventType) {
            case "message" -> handleMessageEvent(event);
            case "postback" -> handlePostbackEvent(event);
            case "follow" -> handleFollowEvent(event);
            case "unfollow" -> handleUnfollowEvent(event);
            case "join" -> handleJoinEvent(event);
            case "leave" -> handleLeaveEvent(event);
            case "memberJoined" -> handleMemberJoinedEvent(event);
            case "memberLeft" -> handleMemberLeftEvent(event);
            default -> log.info("收到未處理的事件類型: {}", eventType);
        }
    }

    private void handleMessageEvent(JsonNode event) {
        try {
            JsonNode message = event.get("message");
            String messageType = message.get("type").asText();
            String replyToken = event.get("replyToken").asText();
            String userId = event.get("source").get("userId").asText();

            switch (messageType) {
                case "text" -> {
                    String userText = message.get("text").asText();
                    log.info("用戶 {} 發送文字訊息: {}", userId, userText);
                    lineBotService.handleTextMessage(userId, userText, replyToken);
                }
                case "image" -> {
                    String messageId = message.get("id").asText();
                    log.info("用戶 {} 發送圖片", userId);
                    lineBotService.handleImageMessage(userId, messageId, replyToken);
                }
                case "video" -> {
                    String messageId = message.get("id").asText();
                    log.info("用戶 {} 發送影片", userId);
                    lineBotService.handleVideoMessage(userId, messageId, replyToken);
                }
                case "audio" -> {
                    String messageId = message.get("id").asText();
                    log.info("用戶 {} 發送音檔", userId);
                    lineBotService.handleAudioMessage(userId, messageId, replyToken);
                }
                case "file" -> {
                    String messageId = message.get("id").asText();
                    String fileName = message.get("fileName").asText();
                    long fileSize = message.get("fileSize").asLong();
                    log.info("用戶 {} 發送檔案: {}", userId, fileName);
                    lineBotService.handleFileMessage(userId, messageId, fileName, fileSize, replyToken);
                }
                case "location" -> {
                    String title = message.has("title") ? message.get("title").asText() : null;
                    String address = message.has("address") ? message.get("address").asText() : null;
                    double latitude = message.get("latitude").asDouble();
                    double longitude = message.get("longitude").asDouble();
                    log.info("用戶 {} 發送位置資訊", userId);
                    lineBotService.handleLocationMessage(userId, title, address, latitude, longitude, replyToken);
                }
                case "sticker" -> {
                    String packageId = message.get("packageId").asText();
                    String stickerId = message.get("stickerId").asText();
                    log.info("用戶 {} 發送貼圖", userId);
                    lineBotService.handleStickerMessage(userId, packageId, stickerId, replyToken);
                }
                default -> {
                    log.info("用戶 {} 發送未處理的訊息類型: {}", userId, messageType);
                    lineBotService.handleTextMessage(userId, "收到您的訊息！", replyToken);
                }
            }
        } catch (Exception e) {
            log.error("處理訊息事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handlePostbackEvent(JsonNode event) {
        try {
            JsonNode postback = event.get("postback");
            if (postback != null) {
                String data = postback.get("data").asText();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                log.info("用戶 {} 點擊按鈕: {}", userId, data);

                // 處理日期時間參數（如果有的話）
                String dateParam = null;
                String timeParam = null;
                String datetimeParam = null;
                
                if (postback.has("params")) {
                    JsonNode params = postback.get("params");
                    dateParam = params.has("date") ? params.get("date").asText() : null;
                    timeParam = params.has("time") ? params.get("time").asText() : null;
                    datetimeParam = params.has("datetime") ? params.get("datetime").asText() : null;
                }

                lineBotService.handlePostback(userId, data, dateParam, timeParam, datetimeParam, replyToken);
            }
        } catch (Exception e) {
            log.error("處理 postback 事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handleFollowEvent(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            String userId = event.get("source").get("userId").asText();

            log.info("用戶 {} 加為好友", userId);
            lineBotService.handleNewFollower(userId, replyToken);

        } catch (Exception e) {
            log.error("處理加好友事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handleUnfollowEvent(JsonNode event) {
        try {
            String userId = event.get("source").get("userId").asText();
            log.info("用戶 {} 取消好友關係", userId);
            lineBotService.handleUnfollow(userId);

        } catch (Exception e) {
            log.error("處理取消好友事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handleJoinEvent(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();

            if ("group".equals(sourceType)) {
                String groupId = source.get("groupId").asText();
                log.info("Bot 加入群組: {}", groupId);
                lineBotService.handleJoinGroup(groupId, replyToken);
                
            } else if ("room".equals(sourceType)) {
                String roomId = source.get("roomId").asText();
                log.info("Bot 加入聊天室: {}", roomId);
                lineBotService.handleJoinRoom(roomId, replyToken);
            }

        } catch (Exception e) {
            log.error("處理加入群組/聊天室事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handleLeaveEvent(JsonNode event) {
        try {
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();

            if ("group".equals(sourceType)) {
                String groupId = source.get("groupId").asText();
                log.info("Bot 離開群組: {}", groupId);
                lineBotService.handleLeaveGroup(groupId);
                
            } else if ("room".equals(sourceType)) {
                String roomId = source.get("roomId").asText();
                log.info("Bot 離開聊天室: {}", roomId);
                lineBotService.handleLeaveRoom(roomId);
            }

        } catch (Exception e) {
            log.error("處理離開群組/聊天室事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handleMemberJoinedEvent(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            JsonNode joined = event.get("joined");
            JsonNode members = joined.get("members");
            int memberCount = members.size();
            
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();

            log.info("群組新成員加入，人數: {}", memberCount);

            if ("group".equals(sourceType)) {
                String groupId = source.get("groupId").asText();
                lineBotService.handleMemberJoinedGroup(groupId, memberCount, replyToken);
            }

        } catch (Exception e) {
            log.error("處理群組新成員加入事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private void handleMemberLeftEvent(JsonNode event) {
        try {
            JsonNode left = event.get("left");
            JsonNode members = left.get("members");
            int memberCount = members.size();
            
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();

            log.info("群組成員離開，人數: {}", memberCount);

            if ("group".equals(sourceType)) {
                String groupId = source.get("groupId").asText();
                lineBotService.handleMemberLeftGroup(groupId, memberCount);
            }

        } catch (Exception e) {
            log.error("處理群組成員離開事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    /**
     * 驗證 LINE webhook 簽名
     */
    private boolean validateSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(channelSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(digest);

            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("簽名驗證時發生錯誤: {}", e.getMessage(), e);
            return false;
        }
    }
}