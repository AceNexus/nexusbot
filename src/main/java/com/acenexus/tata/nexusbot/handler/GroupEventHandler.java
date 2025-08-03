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
public class GroupEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(GroupEventHandler.class);
    private final MessageService messageService;
    private final MessageTemplateService messageTemplateService;

    public void handle(JsonNode event) {
        String eventType = event.get("type").asText();

        switch (eventType) {
            case "join" -> handleJoin(event);
            case "leave" -> handleLeave(event);
            case "memberJoined" -> handleMemberJoined(event);
            case "memberLeft" -> handleMemberLeft(event);
            default -> logger.warn("Unsupported group event type: {}", eventType);
        }
    }

    private void handleJoin(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();

            String joinMessage = messageTemplateService.groupJoinMessage(sourceType);
            messageService.sendReply(replyToken, joinMessage);

        } catch (Exception e) {
            logger.error("Error processing group join event: {}", e.getMessage(), e);
        }
    }

    private void handleLeave(JsonNode event) {
        logger.info("Bot left group/room");
    }

    private void handleMemberJoined(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            int memberCount = event.get("joined").get("members").size();

            String welcomeMessage = messageTemplateService.memberJoinedMessage(memberCount);
            messageService.sendReply(replyToken, welcomeMessage);

        } catch (Exception e) {
            logger.error("Error processing member joined event: {}", e.getMessage(), e);
        }
    }

    private void handleMemberLeft(JsonNode event) {
        logger.info("Group member left");
    }
}