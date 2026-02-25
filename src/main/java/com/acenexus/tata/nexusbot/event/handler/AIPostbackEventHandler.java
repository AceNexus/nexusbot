package com.acenexus.tata.nexusbot.event.handler;

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
                    MODEL_LLAMA_3_1_8B, MODEL_LLAMA_3_3_70B,
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
            case SELECT_MODEL -> aiFacade.showModelSelectionMenu(roomId, roomType);
            case MODEL_LLAMA_3_1_8B ->
                    aiFacade.selectModel(roomId, roomType, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)");
            case MODEL_LLAMA_3_3_70B ->
                    aiFacade.selectModel(roomId, roomType, "llama-3.3-70b-versatile", "Llama 3.3 70B (精準強力)");
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
