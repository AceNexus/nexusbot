package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.EmailInputState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Email 輸入狀態 Repository
 * 管理等待輸入 Email 的聊天室狀態
 */
@Repository
public interface EmailInputStateRepository extends JpaRepository<EmailInputState, String> {

    /**
     * 檢查聊天室是否正在等待 Email 輸入且未過期
     *
     * @param roomId 聊天室 ID
     * @param now    當前時間
     * @return 是否存在有效狀態
     */
    @Query("SELECT COUNT(e) > 0 FROM EmailInputState e WHERE e.roomId = :roomId AND e.expiresAt > :now")
    boolean existsByRoomIdAndNotExpired(@Param("roomId") String roomId, @Param("now") LocalDateTime now);

    /**
     * 刪除已過期的狀態記錄
     *
     * @param now 當前時間
     * @return 刪除的記錄數
     */
    @Modifying
    @Query("DELETE FROM EmailInputState e WHERE e.expiresAt <= :now")
    int deleteExpiredStates(@Param("now") LocalDateTime now);
}
