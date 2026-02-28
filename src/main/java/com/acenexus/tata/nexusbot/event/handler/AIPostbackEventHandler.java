package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.constants.AiModel;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.AIFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMINI_25_FLASH;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;

/**
 * 處理 AI 相關的 Postback 事件
 */
@Component
@RequiredArgsConstructor
public class AIPostbackEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(AIPostbackEventHandler.class);

    private final AIFacade aiFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.POSTBACK) {
            return false;
        }

        String action = event.getPayloadString("action");
        if (action == null) {
            return false;
        }

        return switch (action) {
            case TOGGLE_AI, ENABLE_AI, DISABLE_AI, SELECT_MODEL,
                    MODEL_LLAMA_3_1_8B, MODEL_LLAMA_3_3_70B, MODEL_GEMINI_25_FLASH,
                    CLEAR_HISTORY, CONFIRM_CLEAR_HISTORY -> true;
            default -> false;
        };
    }

    @Override
    public Message handle(LineBotEvent event) {
        String action = event.getPayloadString("action");
        String roomId = event.getRoomId();
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        logger.info("AIPostbackEventHandler handling action: {} for room: {}", action, roomId);

        return switch (action) {
            case TOGGLE_AI -> aiFacade.showSettingsMenu(roomId, roomType);
            case ENABLE_AI -> aiFacade.enableAI(roomId, roomType);
            case DISABLE_AI -> aiFacade.disableAI(roomId, roomType);
            case SELECT_MODEL -> aiFacade.showProviderAndModelMenu(roomId, roomType);
            case MODEL_LLAMA_3_1_8B ->
                    aiFacade.selectModel(roomId, roomType, AiModel.LLAMA_3_1_8B.id, AiModel.LLAMA_3_1_8B.displayName);
            case MODEL_LLAMA_3_3_70B ->
                    aiFacade.selectModel(roomId, roomType, AiModel.LLAMA_3_3_70B.id, AiModel.LLAMA_3_3_70B.displayName);
            case MODEL_GEMINI_25_FLASH ->
                    aiFacade.selectModel(roomId, roomType, AiModel.GEMINI_25_FLASH.id, AiModel.GEMINI_25_FLASH.displayName);
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
