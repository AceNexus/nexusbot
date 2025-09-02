package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ReminderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface ReminderStateRepository extends JpaRepository<ReminderState, String> {

    /**
     * 清除過期的狀態記錄
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ReminderState rs WHERE rs.expiresAt < :now")
    void deleteExpiredStates(LocalDateTime now);
}