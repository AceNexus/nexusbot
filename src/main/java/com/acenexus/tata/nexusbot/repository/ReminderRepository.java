package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    /**
     * 依聊天室ID查詢所有活躍提醒，依提醒時間排序
     */
    @Query("SELECT r FROM Reminder r WHERE r.roomId = :roomId AND r.status = 'ACTIVE' ORDER BY r.reminderTime ASC")
    List<Reminder> findActiveRemindersByRoomId(@Param("roomId") String roomId);

    /**
     * 查詢指定時間範圍內的到期提醒
     */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'ACTIVE' AND r.reminderTime >= :startTime AND r.reminderTime <= :endTime ORDER BY r.reminderTime ASC")
    List<Reminder> findDueReminders(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}