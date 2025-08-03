package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.service.ChatRoomService;
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
    private final ChatRoomService chatRoomService;

    public void handle(JsonNode event) {
        try {
            JsonNode postback = event.get("postback");
            if (postback != null) {
                String data = postback.get("data").asText();
                String replyToken = event.get("replyToken").asText();

                // 獲取來源資訊
                JsonNode source = event.get("source");
                String sourceType = source.get("type").asText();
                String roomId = sourceType.equals("group") ? source.get("groupId").asText() : source.get("userId").asText();

                ChatRoom.RoomType roomType = chatRoomService.determineRoomType(sourceType);

                logger.info("Room {} (type: {}) clicked button: {}", roomId, roomType, data);

                // 處理按鈕回應
                Message response = switch (data) {
                    case "action=toggle_ai" -> {
                        boolean currentStatus = chatRoomService.isAiEnabled(roomId, roomType);
                        yield messageTemplateService.aiSettingsMenu(currentStatus);
                    }
                    case "action=enable_ai" -> {
                        boolean success = chatRoomService.enableAi(roomId, roomType);
                        if (success) {
                            yield messageTemplateService.success("AI 回應功能已啟用！您可以直接與我對話。");
                        } else {
                            yield messageTemplateService.error("啟用 AI 功能時發生錯誤，請稍後再試。");
                        }
                    }
                    case "action=disable_ai" -> {
                        boolean success = chatRoomService.disableAi(roomId, roomType);
                        if (success) {
                            yield messageTemplateService.success("AI 回應功能已關閉。");
                        } else {
                            yield messageTemplateService.error("關閉 AI 功能時發生錯誤，請稍後再試。");
                        }
                    }
                    case "action=help_menu" -> messageTemplateService.helpMenu();
                    case "action=main_menu" -> messageTemplateService.mainMenu();
                    case "action=about" -> messageTemplateService.about();
                    default -> messageTemplateService.postbackResponse(data);
                };

                messageService.sendMessage(replyToken, response);
            }
        } catch (Exception e) {
            logger.error("Error processing postback event: {}", e.getMessage(), e);
        }
    }
}