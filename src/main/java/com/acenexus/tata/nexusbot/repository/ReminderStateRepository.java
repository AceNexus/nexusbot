package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ReminderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReminderStateRepository extends JpaRepository<ReminderState, String> {

    /**
     * 查詢未過期的狀態記錄
     *
     * @param roomId 聊天室 ID
     * @param now    當前時間
     * @return 狀態記錄（可能為空）
     */
    @Query("SELECT rs FROM ReminderState rs WHERE rs.roomId = :roomId AND rs.expiresAt > :now")
    Optional<ReminderState> findByRoomIdAndNotExpired(@Param("roomId") String roomId, @Param("now") LocalDateTime now);

    /**
     * 刪除已過期的狀態記錄
     *
     * @param now 當前時間
     * @return 刪除的記錄數
     */
    @Modifying
    @Query("DELETE FROM ReminderState rs WHERE rs.expiresAt <= :now")
    int deleteExpiredStates(@Param("now") LocalDateTime now);
}