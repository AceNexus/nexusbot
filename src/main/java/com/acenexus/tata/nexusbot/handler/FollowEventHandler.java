package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
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
    private final MessageTemplateProvider messageTemplateProvider;

    public void handleFollow(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            String userId = event.get("source").get("userId").asText();

            logger.info("User {} followed the bot", userId);
            messageService.sendMessage(replyToken, messageTemplateProvider.welcome());

        } catch (Exception e) {
            logger.error("Error processing follow event: {}", e.getMessage(), e);
        }
    }

    public void handleUnfollow(JsonNode event) {
        try {
            String userId = event.get("source").get("userId").asText();
            logger.info("User {} unfollowed the bot", userId);
        } catch (Exception e) {
            logger.error("Error processing unfollow event: {}", e.getMessage(), e);
        }
    }
}