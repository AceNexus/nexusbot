package com.acenexus.tata.nexusbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 負責處理來自 LINE 平台的 webhook
 */
@Slf4j
@RestController
public class LineBotController {

    private final MessagingApiClient messagingApiClient;
    private final ObjectMapper objectMapper;

    public LineBotController(@Value("${line.bot.channel-token}") String channelToken, ObjectMapper objectMapper) {
        this.messagingApiClient = MessagingApiClient.builder(channelToken).build();
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        try {
            log.info("接收到 LINE webhook 請求");

            JsonNode requestBody = objectMapper.readTree(payload);
            JsonNode events = requestBody.get("events");

            for (JsonNode event : events) {
                handleEvent(event);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("處理 webhook 時發生錯誤: {}", e.getMessage(), e);
            return ResponseEntity.ok("OK");
        }
    }

    private void handleEvent(JsonNode event) {
        String eventType = event.get("type").asText();

        if ("message".equals(eventType)) {
            handleMessageEvent(event);
        } else {
            log.info("收到其他類型事件: {}", eventType);
        }
    }

    private void handleMessageEvent(JsonNode event) {
        try {
            JsonNode message = event.get("message");
            String messageType = message.get("type").asText();

            if ("text".equals(messageType)) {
                String userText = message.get("text").asText().toLowerCase().trim();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                log.info("用戶 {} 發送訊息: {}", userId, userText);

                String response = getResponseForMessage(userText, userId);
                sendReply(replyToken, response);
            }
        } catch (Exception e) {
            log.error("處理訊息事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private String getResponseForMessage(String userMessage, String userId) {
        return switch (userMessage) {
            case "menu", "選單", "功能", "help" -> {
                log.info("用戶 {} 請求顯示選單", userId);
                yield getMainMenu();
            }
            default -> "或輸入 'help' 查看使用說明。";
        };
    }

    private String getMainMenu() {
        return "功能選單";
    }

    private void sendReply(String replyToken, String messageText) {
        try {
            TextMessage textMessage = new TextMessage(messageText);
            ReplyMessageRequest request = new ReplyMessageRequest(replyToken, List.of(textMessage), false);

            messagingApiClient.replyMessage(request);
            log.info("成功回覆訊息給用戶，內容長度: {} 字元", messageText.length());

        } catch (Exception e) {
            log.error("回覆訊息時發生錯誤: {}", e.getMessage(), e);
        }
    }
}