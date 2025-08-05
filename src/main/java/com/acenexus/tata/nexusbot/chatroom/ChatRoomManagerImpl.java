package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
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
}