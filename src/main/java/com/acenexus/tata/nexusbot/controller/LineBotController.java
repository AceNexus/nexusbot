package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.service.EventHandlerService;
import com.acenexus.tata.nexusbot.util.SignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LineBotController {

    private final ObjectMapper objectMapper;
    private final EventHandlerService eventHandlerService;
    private final SignatureValidator signatureValidator;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "X-Line-Signature", required = false) String signature) {

        try {
            log.info("Received LINE webhook request, payload size: {} bytes", payload.length());

            if (signature != null && !signatureValidator.validate(payload, signature)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.ok("OK"); // LINE requires 200 even for invalid requests
            }

            JsonNode requestBody = objectMapper.readTree(payload);
            JsonNode events = requestBody.get("events");

            if (events != null && events.isArray() && !events.isEmpty()) {
                log.info("Processing {} events", events.size());
                eventHandlerService.processEvents(events);
            } else {
                log.info("Received empty events array");
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("OK"); // LINE requires 200 even on errors
        }
    }
}