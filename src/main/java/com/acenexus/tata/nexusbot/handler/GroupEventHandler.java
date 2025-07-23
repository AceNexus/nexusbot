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
public class GroupEventHandler {

    private final MessageService messageService;

    public void handle(JsonNode event) {
        String eventType = event.get("type").asText();

        switch (eventType) {
            case "join" -> handleJoin(event);
            case "leave" -> handleLeave(event);
            case "memberJoined" -> handleMemberJoined(event);
            case "memberLeft" -> handleMemberLeft(event);
            default -> log.warn("Unsupported group event type: {}", eventType);
        }
    }

    private void handleJoin(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();

            String joinMessage = BotMessages.getGroupJoinMessage(sourceType);
            messageService.sendReply(replyToken, joinMessage);

        } catch (Exception e) {
            log.error("Error processing group join event: {}", e.getMessage(), e);
        }
    }

    private void handleLeave(JsonNode event) {
        log.info("Bot left group/room");
    }

    private void handleMemberJoined(JsonNode event) {
        try {
            String replyToken = event.get("replyToken").asText();
            int memberCount = event.get("joined").get("members").size();

            String welcomeMessage = BotMessages.getMemberJoinedMessage(memberCount);
            messageService.sendReply(replyToken, welcomeMessage);

        } catch (Exception e) {
            log.error("Error processing member joined event: {}", e.getMessage(), e);
        }
    }

    private void handleMemberLeft(JsonNode event) {
        log.info("Group member left");
    }
}