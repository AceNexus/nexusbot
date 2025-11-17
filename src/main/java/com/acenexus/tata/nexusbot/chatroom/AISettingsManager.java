package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 設定管理器
 * 職責：管理聊天室的 AI 啟用狀態和模型選擇
 */
@Component
@RequiredArgsConstructor
public class AISettingsManager {

    private static final Logger logger = LoggerFactory.getLogger(AISettingsManager.class);
    private static final String DEFAULT_AI_MODEL = "llama-3.1-8b-instant";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomAccessor chatRoomAccessor;

    /**
     * 檢查聊天室的 AI 是否啟用
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return AI 是否啟用，預設為 false
     */
    public boolean isAiEnabled(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, AI disabled by default");
            return false;
        }

        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
        return chatRoom.getAiEnabled();
    }

    /**
     * 啟用聊天室的 AI 回應
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否成功啟用
     */
    @Transactional
    public boolean enableAi(String roomId, ChatRoom.RoomType roomType) {
        try {
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
            chatRoom.setAiEnabled(true);
            chatRoomRepository.save(chatRoom);

            logger.info("AI enabled for room: {} (type: {})", roomId, roomType);
            return true;
        } catch (Exception e) {
            logger.error("Failed to enable AI for room: {}, error: {}", roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 停用聊天室的 AI 回應
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否成功停用
     */
    @Transactional
    public boolean disableAi(String roomId, ChatRoom.RoomType roomType) {
        try {
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
            chatRoom.setAiEnabled(false);
            chatRoomRepository.save(chatRoom);

            logger.info("AI disabled for room: {} (type: {})", roomId, roomType);
            return true;
        } catch (Exception e) {
            logger.error("Failed to disable AI for room: {}, error: {}", roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 獲取聊天室的 AI 模型
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return AI 模型名稱
     */
    public String getAiModel(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return DEFAULT_AI_MODEL;
        }

        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
        return chatRoom.getAiModel() != null ? chatRoom.getAiModel() : DEFAULT_AI_MODEL;
    }

    /**
     * 設定聊天室的 AI 模型
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param model    AI 模型名稱
     * @return 是否成功設定
     */
    @Transactional
    public boolean setAiModel(String roomId, ChatRoom.RoomType roomType, String model) {
        try {
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
            chatRoom.setAiModel(model);
            chatRoomRepository.save(chatRoom);

            logger.info("AI model set to {} for room: {} (type: {})", model, roomId, roomType);
            return true;
        } catch (Exception e) {
            logger.error("Failed to set AI model for room: {}, error: {}", roomId, e.getMessage(), e);
            return false;
        }
    }
}
