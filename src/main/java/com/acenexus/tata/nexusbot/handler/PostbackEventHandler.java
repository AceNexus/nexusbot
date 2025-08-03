package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.service.MessageTemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostbackEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PostbackEventHandler.class);
    private final MessageService messageService;
    private final MessageTemplateService messageTemplateService;

    public void handle(JsonNode event) {
        try {
            JsonNode postback = event.get("postback");
            if (postback != null) {
                String data = postback.get("data").asText();
                String replyToken = event.get("replyToken").asText();
                String userId = event.get("source").get("userId").asText();

                logger.info("User {} clicked button: {}", userId, data);

                // 處理按鈕回應
                Message response = switch (data) {
                    case "action=toggle_ai" -> messageTemplateService.aiSettingsMenu();
                    case "action=medication_menu" -> messageTemplateService.medicationMenu();
                    case "action=help_menu" -> messageTemplateService.helpMenu();
                    case "action=main_menu" -> messageTemplateService.mainMenu();
                    case "action=about" -> messageTemplateService.about();
                    case "action=enable_ai" -> messageTemplateService.success("AI 回應功能已啟用！您可以直接與我對話。");
                    case "action=disable_ai" -> messageTemplateService.success("AI 回應功能已關閉。");
                    default -> messageTemplateService.postbackResponse(data);
                };

                messageService.sendMessage(replyToken, response);
            }
        } catch (Exception e) {
            logger.error("Error processing postback event: {}", e.getMessage(), e);
        }
    }
}