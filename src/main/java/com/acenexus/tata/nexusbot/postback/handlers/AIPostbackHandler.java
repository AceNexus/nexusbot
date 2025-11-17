package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.facade.AIFacade;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
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
 * AI 功能 Handler
 * 職責：純路由，將請求委派給 AIFacade
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class AIPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(AIPostbackHandler.class);

    private final AIFacade aiFacade;

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
            case TOGGLE_AI -> aiFacade.showSettingsMenu(roomId, type);
            case ENABLE_AI -> aiFacade.enableAI(roomId, type);
            case DISABLE_AI -> aiFacade.disableAI(roomId, type);
            case SELECT_MODEL -> aiFacade.showModelSelectionMenu(roomId, type);
            case MODEL_LLAMA_3_1_8B ->
                    aiFacade.selectModel(roomId, type, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)");
            case MODEL_LLAMA_3_3_70B ->
                    aiFacade.selectModel(roomId, type, "llama-3.3-70b-versatile", "Llama 3.3 70B (精準強力)");
            case MODEL_LLAMA3_70B -> aiFacade.selectModel(roomId, type, "llama3-70b-8192", "Llama 3 70B (詳細平衡)");
            case MODEL_GEMMA2_9B -> aiFacade.selectModel(roomId, type, "gemma2-9b-it", "Gemma2 9B (高度創意)");
            case MODEL_DEEPSEEK_R1 ->
                    aiFacade.selectModel(roomId, type, "deepseek-r1-distill-llama-70b", "DeepSeek R1 (邏輯推理)");
            case MODEL_QWEN3_32B -> aiFacade.selectModel(roomId, type, "qwen/qwen3-32b", "Qwen3 32B (多語平衡)");
            case CLEAR_HISTORY -> aiFacade.showClearHistoryConfirmation();
            case CONFIRM_CLEAR_HISTORY -> aiFacade.clearHistory(roomId);
            default -> null;
        };
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
