package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.service.MessageTemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(FollowEventHandler.class);
    private final MessageService messageService;
    private final MessageTemplateService messageTemplateService;

    public void handle(JsonNode event) {
        String eventType = event.get("type").asText();

        switch (eventType) {
            case "follow" -> handleFollow(event);
            case "unfollow" -> handleUnfollow(event);
            default -> logger.warn("Unsupported follow event type: {}", eventType);
        }
    }

    private void handleFollow(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            String userId = event.get("source").get("userId").asText();

            logger.info("User {} followed the bot", userId);
            messageService.sendMessage(replyToken, messageTemplateService.welcome());

        } catch (Exception e) {
            logger.error("Error processing follow event: {}", e.getMessage(), e);
        }
    }

    private void handleUnfollow(JsonNode event) {
        try {
            String userId = event.get("source").get("userId").asText();
            logger.info("User {} unfollowed the bot", userId);
        } catch (Exception e) {
            logger.error("Error processing unfollow event: {}", e.getMessage(), e);
        }
    }
}