package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 聊天訊息資料庫存取層
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}