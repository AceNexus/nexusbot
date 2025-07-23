package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.constants.BotMessages;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowEventHandler {

    private final MessageService messageService;

    public void handle(JsonNode event) {
        String eventType = event.get("type").asText();

        switch (eventType) {
            case "follow" -> handleFollow(event);
            case "unfollow" -> handleUnfollow(event);
            default -> log.warn("Unsupported follow event type: {}", eventType);
        }
    }

    private void handleFollow(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            String userId = event.get("source").get("userId").asText();

            log.info("User {} followed the bot", userId);
            messageService.sendReply(replyToken, BotMessages.WELCOME_MESSAGE);

        } catch (Exception e) {
            log.error("Error processing follow event: {}", e.getMessage(), e);
        }
    }

    private void handleUnfollow(JsonNode event) {
        try {
            String userId = event.get("source").get("userId").asText();
            log.info("User {} unfollowed the bot", userId);
        } catch (Exception e) {
            log.error("Error processing unfollow event: {}", e.getMessage(), e);
        }
    }
}