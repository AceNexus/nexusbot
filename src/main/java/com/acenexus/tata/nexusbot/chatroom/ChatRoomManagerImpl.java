package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import com.acenexus.tata.nexusbot.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 聊天室服務
 * 管理聊天室的 AI 回應狀態
 */
@Service
@RequiredArgsConstructor
public class ChatRoomManagerImpl implements ChatRoomManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomManagerImpl.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 檢查聊天室的 AI 是否啟用
     * 如果聊天室記錄不存在，會自動建立一個預設記錄
     *
     * @param roomId   聊天室 ID (userId 或 groupId)
     * @param roomType 聊天室類型
     * @return AI 是否啟用，預設為 false
     */
    public boolean isAiEnabled(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, AI disabled by default");
            return false;
        }

        // 查詢現有記錄，如果不存在則建立新記錄
        ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
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
            ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
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
            ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
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
    @Override
    public String getAiModel(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return "llama-3.1-8b-instant"; // 預設模型
        }

        ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
        return chatRoom.getAiModel() != null ? chatRoom.getAiModel() : "llama-3.1-8b-instant";
    }

    /**
     * 設定聊天室的 AI 模型
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param model    AI 模型名稱
     * @return 是否成功設定
     */
    @Override
    @Transactional
    public boolean setAiModel(String roomId, ChatRoom.RoomType roomType, String model) {
        try {
            ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
            chatRoom.setAiModel(model);
            chatRoomRepository.save(chatRoom);

            logger.info("AI model set to {} for room: {} (type: {})", model, roomId, roomType);
            return true;
        } catch (Exception e) {
            logger.error("Failed to set AI model for room: {}, error: {}", roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 清除聊天室的歷史對話記錄
     *
     * @param roomId 聊天室 ID
     */
    @Override
    @Transactional
    public void clearChatHistory(String roomId) {
        try {
            chatMessageRepository.softDeleteByRoomId(roomId);
            logger.info("Cleared chat history for room: {}", roomId);
        } catch (Exception e) {
            logger.error("Failed to clear chat history for room: {}, error: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 獲取或建立聊天室記錄
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 聊天室實體
     */
    private ChatRoom getOrCreateChatRoom(String roomId, ChatRoom.RoomType roomType) {
        return chatRoomRepository.findByRoomId(roomId)
                .orElseGet(() -> {
                    ChatRoom newChatRoom = ChatRoom.builder()
                            .roomId(roomId)
                            .roomType(roomType)
                            .aiEnabled(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    logger.info("Creating new chat room record: {} (type: {})", roomId, roomType);
                    return chatRoomRepository.save(newChatRoom);
                });
    }

    /**
     * 根據來源類型判斷聊天室類型
     */
    public ChatRoom.RoomType determineRoomType(String sourceType) {
        return "group".equals(sourceType) ? ChatRoom.RoomType.GROUP : ChatRoom.RoomType.USER;
    }

    /**
     * 設定聊天室的管理員狀態
     */
    @Override
    @Transactional
    public boolean setAdminStatus(String roomId, ChatRoom.RoomType roomType, boolean isAdmin) {
        try {
            ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
            chatRoom.setIsAdmin(isAdmin);

            chatRoomRepository.save(chatRoom);
            logger.info("Admin status updated for room {}: isAdmin={}", roomId, isAdmin);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update admin status for room {}: {}", roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 設定聊天室的認證等待狀態
     */
    @Override
    @Transactional
    public boolean setAuthPending(String roomId, ChatRoom.RoomType roomType, boolean authPending) {
        try {
            ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
            chatRoom.setAuthPending(authPending);

            chatRoomRepository.save(chatRoom);
            logger.info("Auth pending status updated for room {}: authPending={}", roomId, authPending);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update auth pending status for room {}: {}", roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 檢查聊天室是否正在等待密碼輸入
     */
    @Override
    public boolean isAuthPending(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, not auth pending");
            return false;
        }

        ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
        return chatRoom.getAuthPending();
    }

    /**
     * 檢查聊天室是否為管理員
     */
    @Override
    public boolean isAdminRoom(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, not admin room");
            return false;
        }

        ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
        return Boolean.TRUE.equals(chatRoom.getIsAdmin());
    }

    /**
     * 設定聊天室的廁所搜尋狀態（等待位置）
     */
    @Override
    @Transactional
    public boolean setWaitingForToiletSearch(String roomId, ChatRoom.RoomType roomType, boolean waitingForToiletSearch) {
        try {
            ChatRoom chatRoom = getOrCreateChatRoom(roomId, roomType);
            chatRoom.setWaitingForLocation(waitingForToiletSearch);

            chatRoomRepository.save(chatRoom);
            logger.info("Toilet search waiting status updated for room {}: waitingForToiletSearch={}", roomId, waitingForToiletSearch);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update toilet search waiting status for room {}: {}", roomId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 檢查聊天室是否正在等待位置進行廁所搜尋
     */
    @Override
    public boolean isWaitingForToiletSearch(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, not waiting for toilet search");
            return false;
        }

        // 對於查詢操作，如果記錄不存在直接返回 false，不需要建立新記錄
        return chatRoomRepository.findByRoomId(roomId)
                .map(chatRoom -> Boolean.TRUE.equals(chatRoom.getWaitingForLocation()))
                .orElse(false);
    }

    /**
     * 設定聊天室的廁所搜尋狀態（僅修改現有記錄）
     */
    @Override
    @Transactional
    public boolean updateWaitingForToiletSearch(String roomId, boolean waitingForToiletSearch) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, cannot update toilet search status");
            return false;
        }

        try {
            return chatRoomRepository.findByRoomId(roomId)
                    .map(chatRoom -> {
                        chatRoom.setWaitingForLocation(waitingForToiletSearch);
                        chatRoomRepository.save(chatRoom);
                        logger.info("Updated toilet search waiting status for room {}: waitingForToiletSearch={}", roomId, waitingForToiletSearch);
                        return true;
                    })
                    .orElseGet(() -> {
                        logger.warn("Cannot update toilet search status for non-existent room: {}", roomId);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Failed to update toilet search waiting status for room {}: {}", roomId, e.getMessage(), e);
            return false;
        }
    }
}