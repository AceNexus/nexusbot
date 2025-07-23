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
public class PostbackEventHandler {

    private final MessageService messageService;

    public void handle(JsonNode event) {
        try {
            JsonNode postback = event.get("postback");
            if (postback != null) {
                String data = postback.get("data").asText();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                log.info("User {} clicked button: {}", userId, data);

                String response = switch (data) {
                    case "action_help" -> BotMessages.getHelpMessage();
                    case "action_menu" -> BotMessages.getMenuMessage();
                    case "action_about" -> BotMessages.ABOUT;
                    default -> BotMessages.getPostbackResponse(data);
                };

                messageService.sendReply(replyToken, response);
            }
        } catch (Exception e) {
            log.error("Error processing postback event: {}", e.getMessage(), e);
        }
    }
}