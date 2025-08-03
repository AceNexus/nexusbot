package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.constants.BotMessages;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostbackEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PostbackEventHandler.class);
    private final MessageService messageService;

    public void handle(JsonNode event) {
        try {
            JsonNode postback = event.get("postback");
            if (postback != null) {
                String data = postback.get("data").asText();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                logger.info("User {} clicked button: {}", userId, data);

                // 處理功能回應
                String response = switch (data) {
                    case "action=toggle_ai" -> "AI 回應功能目前已啟用，您可以直接與我對話！";
                    default -> BotMessages.getPostbackResponse(data);
                };

                messageService.sendReply(replyToken, response);
            }
        } catch (Exception e) {
            logger.error("Error processing postback event: {}", e.getMessage(), e);
        }
    }
}