package com.acenexus.tata.nexusbot.handler.postback;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_DEEPSEEK_R1;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMMA2_9B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_QWEN3_32B;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;

/**
 * AI 功能 Handler - 處理 AI 開關、模型選擇、對話歷史管理
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class AIPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(AIPostbackHandler.class);

    private final ChatRoomManager chatRoomManager;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(String action) {
        return switch (action) {
            case TOGGLE_AI, ENABLE_AI, DISABLE_AI, SELECT_MODEL,
                    MODEL_LLAMA_3_1_8B, MODEL_LLAMA_3_3_70B, MODEL_LLAMA3_70B,
                    MODEL_GEMMA2_9B, MODEL_DEEPSEEK_R1, MODEL_QWEN3_32B,
                    CLEAR_HISTORY, CONFIRM_CLEAR_HISTORY -> true;
            default -> false;
        };
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("AIPostbackHandler handling action: {} for room: {}", action, roomId);

        ChatRoom.RoomType type = ChatRoom.RoomType.valueOf(roomType);

        return switch (action) {
            case TOGGLE_AI -> {
                boolean currentStatus = chatRoomManager.isAiEnabled(roomId, type);
                logger.debug("Current AI status for room {}: {}", roomId, currentStatus);
                yield messageTemplateProvider.aiSettingsMenu(currentStatus);
            }

            case ENABLE_AI -> {
                boolean success = chatRoomManager.enableAi(roomId, type);
                if (success) {
                    logger.info("AI enabled for room: {}", roomId);
                    yield messageTemplateProvider.success("AI 回應功能已啟用！您可以直接與我對話。");
                } else {
                    logger.error("Failed to enable AI for room: {}", roomId);
                    yield messageTemplateProvider.error("啟用 AI 功能時發生錯誤，請稍後再試。");
                }
            }

            case DISABLE_AI -> {
                boolean success = chatRoomManager.disableAi(roomId, type);
                if (success) {
                    logger.info("AI disabled for room: {}", roomId);
                    yield messageTemplateProvider.success("AI 回應功能已關閉。");
                } else {
                    logger.error("Failed to disable AI for room: {}", roomId);
                    yield messageTemplateProvider.error("關閉 AI 功能時發生錯誤，請稍後再試。");
                }
            }

            case SELECT_MODEL -> {
                String currentModel = chatRoomManager.getAiModel(roomId, type);
                logger.debug("Current AI model for room {}: {}", roomId, currentModel);
                yield messageTemplateProvider.aiModelSelectionMenu(currentModel);
            }

            case MODEL_LLAMA_3_1_8B -> handleModelSelection(roomId, type, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)");

            case MODEL_LLAMA_3_3_70B -> handleModelSelection(roomId, type, "llama-3.3-70b-versatile", "Llama 3.3 70B (精準強力)");

            case MODEL_LLAMA3_70B -> handleModelSelection(roomId, type, "llama3-70b-8192", "Llama 3 70B (詳細平衡)");

            case MODEL_GEMMA2_9B -> handleModelSelection(roomId, type, "gemma2-9b-it", "Gemma2 9B (高度創意)");

            case MODEL_DEEPSEEK_R1 -> handleModelSelection(roomId, type, "deepseek-r1-distill-llama-70b", "DeepSeek R1 (邏輯推理)");

            case MODEL_QWEN3_32B -> handleModelSelection(roomId, type, "qwen/qwen3-32b", "Qwen3 32B (多語平衡)");

            case CLEAR_HISTORY -> {
                logger.debug("Showing clear history confirmation for room: {}", roomId);
                yield messageTemplateProvider.clearHistoryConfirmation();
            }

            case CONFIRM_CLEAR_HISTORY -> {
                chatRoomManager.clearChatHistory(roomId);
                logger.info("Chat history cleared for room: {}", roomId);
                yield messageTemplateProvider.success("歷史對話記錄已清除。");
            }

            default -> {
                logger.warn("Unexpected action in AIPostbackHandler: {}", action);
                yield null;
            }
        };
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

    @Override
    public int getPriority() {
        return 2;
    }
}
