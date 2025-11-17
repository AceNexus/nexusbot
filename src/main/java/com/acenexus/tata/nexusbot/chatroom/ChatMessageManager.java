package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 聊天訊息管理器
 * 職責：管理聊天室的歷史訊息
 */
@Component
@RequiredArgsConstructor
public class ChatMessageManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageManager.class);

    private final ChatMessageRepository chatMessageRepository;

    /**
     * 清除聊天室的歷史對話記錄
     *
     * @param roomId 聊天室 ID
     */
    @Transactional
    public void clearChatHistory(String roomId) {
        try {
            chatMessageRepository.softDeleteByRoomId(roomId);
            logger.info("Cleared chat history for room: {}", roomId);
        } catch (Exception e) {
            logger.error("Failed to clear chat history for room: {}, error: {}", roomId, e.getMessage(), e);
        }
    }
}
