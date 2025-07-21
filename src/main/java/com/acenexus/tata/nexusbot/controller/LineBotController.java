package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.service.LineEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LineBotController {

    private final ObjectMapper objectMapper;
    private final LineEventService lineEventService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        try {
            log.info("接收到 LINE webhook 請求，payload 長度: {} bytes", payload.length());

            JsonNode requestBody = objectMapper.readTree(payload);
            JsonNode events = requestBody.get("events");

            if (events != null && events.isArray() && !events.isEmpty()) {
                log.info("處理 {} 個事件", events.size());

                for (JsonNode event : events) {
                    try {
                        lineEventService.handleEvent(event);
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
            // 請參考專案中 Line_Bot_Verify_webhook_URL.png 圖片
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        }
    }
}