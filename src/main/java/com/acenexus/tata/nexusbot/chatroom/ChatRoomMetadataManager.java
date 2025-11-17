package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 聊天室元數據管理器
 * 職責：管理聊天室的管理員狀態、認證狀態和功能狀態（如廁所搜尋）
 */
@Component
@RequiredArgsConstructor
public class ChatRoomMetadataManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomMetadataManager.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomAccessor chatRoomAccessor;

    /**
     * 設定聊天室的管理員狀態
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param isAdmin  是否為管理員
     * @return 是否成功設定
     */
    @Transactional
    public boolean setAdminStatus(String roomId, ChatRoom.RoomType roomType, boolean isAdmin) {
        try {
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
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
     * 檢查聊天室是否為管理員
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否為管理員聊天室
     */
    public boolean isAdminRoom(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, not admin room");
            return false;
        }

        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
        return Boolean.TRUE.equals(chatRoom.getIsAdmin());
    }

    /**
     * 設定聊天室的認證等待狀態
     *
     * @param roomId      聊天室 ID
     * @param roomType    聊天室類型
     * @param authPending 是否正在等待密碼輸入
     * @return 是否成功設定
     */
    @Transactional
    public boolean setAuthPending(String roomId, ChatRoom.RoomType roomType, boolean authPending) {
        try {
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
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
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否正在等待密碼輸入
     */
    public boolean isAuthPending(String roomId, ChatRoom.RoomType roomType) {
        if (roomId == null || roomId.trim().isEmpty()) {
            logger.warn("Room ID is null or empty, not auth pending");
            return false;
        }

        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
        return chatRoom.getAuthPending();
    }

    /**
     * 設定聊天室的廁所搜尋狀態（等待位置）
     *
     * @param roomId                 聊天室 ID
     * @param roomType               聊天室類型
     * @param waitingForToiletSearch 是否正在等待位置進行廁所搜尋
     * @return 是否成功設定
     */
    @Transactional
    public boolean setWaitingForToiletSearch(String roomId, ChatRoom.RoomType roomType, boolean waitingForToiletSearch) {
        try {
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, roomType);
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
     *
     * @param roomId 聊天室 ID
     * @return 是否正在等待位置進行廁所搜尋
     */
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
     *
     * @param roomId                 聊天室 ID
     * @param waitingForToiletSearch 是否正在等待位置進行廁所搜尋
     * @return 是否成功設定
     */
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
