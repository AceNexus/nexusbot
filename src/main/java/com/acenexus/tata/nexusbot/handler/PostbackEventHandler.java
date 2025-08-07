package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;

@Component
@RequiredArgsConstructor
public class PostbackEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PostbackEventHandler.class);
    private final MessageService messageService;
    private final MessageTemplateProvider messageTemplateProvider;
    private final ChatRoomManager chatRoomManager;

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

                ChatRoom.RoomType roomType = chatRoomManager.determineRoomType(sourceType);

                logger.info("Room {} (type: {}) clicked button: {}", roomId, roomType, data);

                // 處理按鈕回應
                Message response = switch (data) {
                    case TOGGLE_AI -> {
                        boolean currentStatus = chatRoomManager.isAiEnabled(roomId, roomType);
                        yield messageTemplateProvider.aiSettingsMenu(currentStatus);
                    }
                    case ENABLE_AI -> {
                        boolean success = chatRoomManager.enableAi(roomId, roomType);
                        if (success) {
                            yield messageTemplateProvider.success("AI 回應功能已啟用！您可以直接與我對話。");
                        } else {
                            yield messageTemplateProvider.error("啟用 AI 功能時發生錯誤，請稍後再試。");
                        }
                    }
                    case DISABLE_AI -> {
                        boolean success = chatRoomManager.disableAi(roomId, roomType);
                        if (success) {
                            yield messageTemplateProvider.success("AI 回應功能已關閉。");
                        } else {
                            yield messageTemplateProvider.error("關閉 AI 功能時發生錯誤，請稍後再試。");
                        }
                    }
                    case HELP_MENU -> messageTemplateProvider.helpMenu();
                    case MAIN_MENU -> messageTemplateProvider.mainMenu();
                    case ABOUT -> messageTemplateProvider.about();
                    default -> messageTemplateProvider.postbackResponse(data);
                };

                messageService.sendMessage(replyToken, response);
            }
        } catch (Exception e) {
            logger.error("Error processing postback event: {}", e.getMessage(), e);
        }
    }
}