package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 聊天室存取輔助類別
 * 職責：提供聊天室實體的基本存取操作
 */
@Component
@RequiredArgsConstructor
public class ChatRoomAccessor {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomAccessor.class);

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 獲取或建立聊天室記錄
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 聊天室實體
     */
    public ChatRoom getOrCreateChatRoom(String roomId, ChatRoom.RoomType roomType) {
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
     *
     * @param sourceType 來源類型
     * @return 聊天室類型
     */
    public ChatRoom.RoomType determineRoomType(String sourceType) {
        return "group".equals(sourceType) ? ChatRoom.RoomType.GROUP : ChatRoom.RoomType.USER;
    }
}
