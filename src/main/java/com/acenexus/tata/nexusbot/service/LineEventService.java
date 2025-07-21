package com.acenexus.tata.nexusbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineEventService {

    private final MessageService messageService;

    public void handleEvent(JsonNode event) {
        String eventType = event.get("type").asText();
        switch (eventType) {
            case "message" -> handleMessageEvent(event);
            case "postback" -> handlePostbackEvent(event);
            default -> log.info("收到未處理的事件類型: {}", eventType);
        }
    }

    /**
     * 處理用戶訊息事件
     */
    private void handleMessageEvent(JsonNode event) {
        try {
            JsonNode message = event.get("message");
            String messageType = message.get("type").asText();

            if ("text".equals(messageType)) {
                String userText = message.get("text").asText().toLowerCase().trim();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                log.info("用戶 {} 發送訊息: {}", userId, userText);

                String response = "測試";
                messageService.sendReply(replyToken, response);
            }
        } catch (Exception e) {
            log.error("處理訊息事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    /**
     * 處理按鈕點擊事件
     */
    private void handlePostbackEvent(JsonNode event) {
        try {
            JsonNode postback = event.get("postback");
            if (postback != null) {
                String data = postback.get("data").asText();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                log.info("用戶 {} 點擊按鈕: {}", userId, data);

                String response = "測試";
                messageService.sendReply(replyToken, response);
            }
        } catch (Exception e) {
            log.error("處理 postback 事件時發生錯誤: {}", e.getMessage(), e);
        }
    }
}