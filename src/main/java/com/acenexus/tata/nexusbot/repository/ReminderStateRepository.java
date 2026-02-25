package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ReminderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReminderStateRepository extends JpaRepository<ReminderState, String> {

    /**
     * 刪除已過期的狀態記錄
     *
     * @param now 當前時間
     * @return 刪除的記錄數
     */
    @Modifying
    @Query("DELETE FROM ReminderState rs WHERE rs.expiresAt < :now")
    int deleteExpiredStates(@Param("now") LocalDateTime now);
}