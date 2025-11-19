package com.acenexus.tata.nexusbot.chatroom.impl;

import com.acenexus.tata.nexusbot.chatroom.AISettingsManager;
import com.acenexus.tata.nexusbot.chatroom.ChatMessageManager;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomAccessor;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomMetadataManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 聊天室管理服務實作（協調者模式）
 * 職責：將請求委派給專門的 Manager
 */
@Service
@RequiredArgsConstructor
public class ChatRoomManagerImpl implements ChatRoomManager {

    private final AISettingsManager aiSettingsManager;
    private final ChatMessageManager chatMessageManager;
    private final ChatRoomMetadataManager chatRoomMetadataManager;
    private final ChatRoomAccessor chatRoomAccessor;

    // ==================== AI Settings ====================

    @Override
    public boolean isAiEnabled(String roomId, ChatRoom.RoomType roomType) {
        return aiSettingsManager.isAiEnabled(roomId, roomType);
    }

    @Override
    public boolean enableAi(String roomId, ChatRoom.RoomType roomType) {
        return aiSettingsManager.enableAi(roomId, roomType);
    }

    @Override
    public boolean disableAi(String roomId, ChatRoom.RoomType roomType) {
        return aiSettingsManager.disableAi(roomId, roomType);
    }

    @Override
    public String getAiModel(String roomId, ChatRoom.RoomType roomType) {
        return aiSettingsManager.getAiModel(roomId, roomType);
    }

    @Override
    public boolean setAiModel(String roomId, ChatRoom.RoomType roomType, String model) {
        return aiSettingsManager.setAiModel(roomId, roomType, model);
    }

    // ==================== Chat Messages ====================

    @Override
    public void clearChatHistory(String roomId) {
        chatMessageManager.clearChatHistory(roomId);
    }

    // ==================== Room Metadata ====================

    @Override
    public boolean setAdminStatus(String roomId, ChatRoom.RoomType roomType, boolean isAdmin) {
        return chatRoomMetadataManager.setAdminStatus(roomId, roomType, isAdmin);
    }

    @Override
    public boolean isAdminRoom(String roomId, ChatRoom.RoomType roomType) {
        return chatRoomMetadataManager.isAdminRoom(roomId, roomType);
    }

    @Override
    public boolean setAuthPending(String roomId, ChatRoom.RoomType roomType, boolean authPending) {
        return chatRoomMetadataManager.setAuthPending(roomId, roomType, authPending);
    }

    @Override
    public boolean isAuthPending(String roomId, ChatRoom.RoomType roomType) {
        return chatRoomMetadataManager.isAuthPending(roomId, roomType);
    }

    @Override
    public boolean setWaitingForToiletSearch(String roomId, ChatRoom.RoomType roomType, boolean waitingForToiletSearch) {
        return chatRoomMetadataManager.setWaitingForToiletSearch(roomId, roomType, waitingForToiletSearch);
    }

    @Override
    public boolean isWaitingForToiletSearch(String roomId) {
        return chatRoomMetadataManager.isWaitingForToiletSearch(roomId);
    }

    @Override
    public boolean updateWaitingForToiletSearch(String roomId, boolean waitingForToiletSearch) {
        return chatRoomMetadataManager.updateWaitingForToiletSearch(roomId, waitingForToiletSearch);
    }

    // ==================== Utility ====================

    @Override
    public ChatRoom.RoomType determineRoomType(String sourceType) {
        return chatRoomAccessor.determineRoomType(sourceType);
    }
}