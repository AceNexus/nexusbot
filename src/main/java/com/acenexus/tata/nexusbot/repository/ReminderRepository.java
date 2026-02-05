package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    /**
     * 依聊天室ID查詢所有活躍提醒，依提醒時間排序
     */
    @Query("SELECT r FROM Reminder r WHERE r.roomId = :roomId AND r.status = 'ACTIVE' ORDER BY r.reminderTimeInstant ASC")
    List<Reminder> findActiveRemindersByRoomId(@Param("roomId") String roomId);

    /**
     * 查詢指定時間之前的所有到期提醒（不設下限，避免重啟後遺漏）
     */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'ACTIVE' AND r.reminderTimeInstant < :endInstant ORDER BY r.reminderTimeInstant ASC")
    List<Reminder> findDueRemindersByInstantBefore(@Param("endInstant") Long endInstant);
}