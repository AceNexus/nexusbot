package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.handler.FollowEventHandler;
import com.acenexus.tata.nexusbot.handler.GroupEventHandler;
import com.acenexus.tata.nexusbot.handler.MessageEventHandler;
import com.acenexus.tata.nexusbot.postback.PostbackDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventHandlerService {
    private static final Logger logger = LoggerFactory.getLogger(EventHandlerService.class);
    private final MessageEventHandler messageEventHandler;
    private final PostbackDispatcher postbackDispatcher;
    private final FollowEventHandler followEventHandler;
    private final GroupEventHandler groupEventHandler;

    public void processEvents(JsonNode events) {
        for (JsonNode event : events) {
            processEvent(event);
        }
    }

    private void processEvent(JsonNode event) {
        try {
            String eventType = event.get("type").asText();
            logger.debug("Processing event type: {}", eventType);

            switch (eventType) {
                case "message" -> messageEventHandler.handle(event);
                case "postback" -> postbackDispatcher.handle(event);
                case "follow" -> followEventHandler.handleFollow(event);
                case "unfollow" -> followEventHandler.handleUnfollow(event);
                case "join" -> groupEventHandler.handleJoin(event);
                case "leave" -> groupEventHandler.handleLeave(event);
                case "memberJoined" -> groupEventHandler.handleMemberJoined(event);
                case "memberLeft" -> groupEventHandler.handleMemberLeft(event);
                default -> logger.info("Unhandled event type: {}", eventType);
            }

        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}