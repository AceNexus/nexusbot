package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI 功能 Facade 實作
 */
@Service
@RequiredArgsConstructor
public class AIFacadeImpl implements AIFacade {

    private static final Logger logger = LoggerFactory.getLogger(AIFacadeImpl.class);

    private final ChatRoomManager chatRoomManager;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public Message showSettingsMenu(String roomId, ChatRoom.RoomType roomType) {
        boolean currentStatus = chatRoomManager.isAiEnabled(roomId, roomType);
        logger.debug("Current AI status for room {}: {}", roomId, currentStatus);
        return messageTemplateProvider.aiSettingsMenu(currentStatus);
    }

    @Override
    public Message enableAI(String roomId, ChatRoom.RoomType roomType) {
        boolean success = chatRoomManager.enableAi(roomId, roomType);
        if (success) {
            logger.info("AI enabled for room: {}", roomId);
            return messageTemplateProvider.success("AI 回應功能已啟用！您可以直接與我對話。");
        } else {
            logger.error("Failed to enable AI for room: {}", roomId);
            return messageTemplateProvider.error("啟用 AI 功能時發生錯誤，請稍後再試。");
        }
    }

    @Override
    public Message disableAI(String roomId, ChatRoom.RoomType roomType) {
        boolean success = chatRoomManager.disableAi(roomId, roomType);
        if (success) {
            logger.info("AI disabled for room: {}", roomId);
            return messageTemplateProvider.success("AI 回應功能已關閉。");
        } else {
            logger.error("Failed to disable AI for room: {}", roomId);
            return messageTemplateProvider.error("關閉 AI 功能時發生錯誤，請稍後再試。");
        }
    }

    @Override
    public Message showModelSelectionMenu(String roomId, ChatRoom.RoomType roomType) {
        String currentModel = chatRoomManager.getAiModel(roomId, roomType);
        logger.debug("Current AI model for room {}: {}", roomId, currentModel);
        return messageTemplateProvider.aiModelSelectionMenu(currentModel);
    }

    @Override
    public Message selectModel(String roomId, ChatRoom.RoomType roomType, String modelId, String modelName) {
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
    public Message showClearHistoryConfirmation() {
        logger.debug("Showing clear history confirmation");
        return messageTemplateProvider.clearHistoryConfirmation();
    }

    @Override
    public Message clearHistory(String roomId) {
        chatRoomManager.clearChatHistory(roomId);
        logger.info("Chat history cleared for room: {}", roomId);
        return messageTemplateProvider.success("歷史對話記錄已清除。");
    }
}
