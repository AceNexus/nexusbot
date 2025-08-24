package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天訊息資料庫存取層
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 查詢指定聊天室最近的 N 筆訊息記錄
     *
     * @param roomId 聊天室 ID
     * @param limit  限制筆數
     * @return 最近的訊息記錄列表（按時間降序）
     */
    @Query(value = """
            SELECT * FROM chat_messages 
            WHERE room_id = :roomId AND deleted_at IS NULL
            ORDER BY created_at DESC 
            LIMIT :limit
            """, nativeQuery = true)
    List<ChatMessage> findRecentMessagesDesc(@Param("roomId") String roomId,
                                             @Param("limit") int limit);

    /**
     * 軟刪除指定聊天室的所有歷史記錄
     *
     * @param roomId 聊天室 ID
     */
    @Modifying
    @Query("UPDATE ChatMessage SET deletedAt = CURRENT_TIMESTAMP WHERE roomId = :roomId AND deletedAt IS NULL")
    void softDeleteByRoomId(@Param("roomId") String roomId);
}