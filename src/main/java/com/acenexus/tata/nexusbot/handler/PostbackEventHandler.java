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
import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_DEEPSEEK_R1;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMMA2_9B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_QWEN3_32B;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
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
                    case SELECT_MODEL -> {
                        String currentModel = chatRoomManager.getAiModel(roomId, roomType);
                        yield messageTemplateProvider.aiModelSelectionMenu(currentModel);
                    }
                    case MODEL_LLAMA_3_1_8B ->
                            handleModelSelection(roomId, roomType, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)");
                    case MODEL_LLAMA_3_3_70B ->
                            handleModelSelection(roomId, roomType, "llama-3.3-70b-versatile", "Llama 3.3 70B (精準強力)");
                    case MODEL_LLAMA3_70B ->
                            handleModelSelection(roomId, roomType, "llama3-70b-8192", "Llama 3 70B (詳細平衡)");
                    case MODEL_GEMMA2_9B ->
                            handleModelSelection(roomId, roomType, "gemma2-9b-it", "Gemma2 9B (高度創意)");
                    case MODEL_DEEPSEEK_R1 ->
                            handleModelSelection(roomId, roomType, "deepseek-r1-distill-llama-70b", "DeepSeek R1 (邏輯推理)");
                    case MODEL_QWEN3_32B ->
                            handleModelSelection(roomId, roomType, "qwen/qwen3-32b", "Qwen3 32B (多語平衡)");
                    case CLEAR_HISTORY -> messageTemplateProvider.clearHistoryConfirmation();
                    case CONFIRM_CLEAR_HISTORY -> {
                        chatRoomManager.clearChatHistory(roomId);
                        yield messageTemplateProvider.success("歷史對話記錄已清除。");
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

    private Message handleModelSelection(String roomId, ChatRoom.RoomType roomType, String modelId, String modelName) {
        boolean success = chatRoomManager.setAiModel(roomId, roomType, modelId);
        if (success) {
            logger.info("AI model changed to {} for room {} (type: {})", modelId, roomId, roomType);
            return messageTemplateProvider.success("AI 模型已切換至：" + modelName);
        } else {
            logger.error("Failed to change AI model to {} for room {} (type: {})", modelId, roomId, roomType);
            return messageTemplateProvider.error("切換 AI 模型時發生錯誤，請稍後再試。");
        }
    }
}