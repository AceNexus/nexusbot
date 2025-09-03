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
    @Query("SELECT r FROM Reminder r WHERE r.roomId = :roomId AND r.status = 'ACTIVE' ORDER BY r.reminderTime ASC")
    List<Reminder> findActiveRemindersByRoomId(@Param("roomId") String roomId);
}