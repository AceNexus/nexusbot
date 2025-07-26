package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.service.EventHandlerService;
import com.acenexus.tata.nexusbot.util.SignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LineBotController {
    private static final Logger logger = LoggerFactory.getLogger(LineBotController.class);
    private final ObjectMapper objectMapper;
    private final EventHandlerService eventHandlerService;
    private final SignatureValidator signatureValidator;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "X-Line-Signature", required = false) String signature) throws Exception {

        logger.info("Received LINE webhook request, payload size: {} bytes", payload.length());

        if (signature != null && !signatureValidator.validate(payload, signature)) {
            logger.warn("Invalid webhook signature");
            return ResponseEntity.ok("OK");
        }

        JsonNode requestBody = objectMapper.readTree(payload);
        JsonNode events = requestBody.get("events");

        if (events != null && events.isArray() && !events.isEmpty()) {
            logger.info("Processing {} events", events.size());
            eventHandlerService.processEvents(events);
        } else {
            logger.info("Received empty events array");
        }

        return ResponseEntity.ok("OK");
    }
}