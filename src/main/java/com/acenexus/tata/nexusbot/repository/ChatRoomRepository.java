package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 聊天室資料庫存取層
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 根據聊天室 ID 查詢聊天室
     *
     * @param roomId 聊天室 ID (userId 或 groupId)
     * @return 聊天室資訊
     */
    Optional<ChatRoom> findByRoomId(String roomId);

    /**
     * 查詢聊天室的 AI 啟用狀態
     */
    @Query("SELECT c.aiEnabled FROM ChatRoom c WHERE c.roomId = :roomId")
    Optional<Boolean> findAiEnabledByRoomId(@Param("roomId") String roomId);
}