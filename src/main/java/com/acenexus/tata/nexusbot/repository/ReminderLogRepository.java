package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    @Query("SELECT rl FROM ReminderLog rl WHERE rl.reminderId = :reminderId AND rl.status = 'SENT' ORDER BY rl.sentTime DESC LIMIT 1")
    Optional<ReminderLog> findLatestSentLogByReminderId(@Param("reminderId") Long reminderId);

    @Query("SELECT rl FROM ReminderLog rl WHERE rl.reminderId IN :reminderIds AND rl.status = 'SENT' AND rl.userResponseStatus IS NOT NULL")
    List<ReminderLog> findUserResponsesByReminderIds(@Param("reminderIds") List<Long> reminderIds);

    /**
     * 根據確認 Token 查詢 ReminderLog
     */
    Optional<ReminderLog> findByConfirmationToken(String confirmationToken);

    /**
     * 查詢指定提醒的最新日誌（用於檢查確認狀態）
     */
    @Query("SELECT rl FROM ReminderLog rl WHERE rl.reminderId = :reminderId ORDER BY rl.createdAt DESC LIMIT 1")
    Optional<ReminderLog> findLatestByReminderId(@Param("reminderId") Long reminderId);

    /**
     * 查詢今日已發送的提醒記錄
     */
    @Query("SELECT rl FROM ReminderLog rl WHERE rl.roomId = :roomId AND rl.status = 'SENT' AND rl.sentTime >= :startOfDay AND rl.sentTime < :endOfDay ORDER BY rl.sentTime DESC")
    List<ReminderLog> findTodaysSentLogs(@Param("roomId") String roomId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}