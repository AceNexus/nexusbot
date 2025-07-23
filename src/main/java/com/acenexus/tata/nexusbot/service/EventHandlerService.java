package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.handler.FollowEventHandler;
import com.acenexus.tata.nexusbot.handler.GroupEventHandler;
import com.acenexus.tata.nexusbot.handler.MessageEventHandler;
import com.acenexus.tata.nexusbot.handler.PostbackEventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventHandlerService {

    private final MessageEventHandler messageEventHandler;
    private final PostbackEventHandler postbackEventHandler;
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
            log.debug("Processing event type: {}", eventType);

            switch (eventType) {
                case "message" -> messageEventHandler.handle(event);
                case "postback" -> postbackEventHandler.handle(event);
                case "follow" -> followEventHandler.handle(event);
                case "unfollow" -> followEventHandler.handle(event);
                case "join" -> groupEventHandler.handle(event);
                case "leave" -> groupEventHandler.handle(event);
                case "memberJoined" -> groupEventHandler.handle(event);
                case "memberLeft" -> groupEventHandler.handle(event);
                default -> log.info("Unhandled event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}