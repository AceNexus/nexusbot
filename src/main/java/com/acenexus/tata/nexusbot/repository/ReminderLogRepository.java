package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    @Query("SELECT rl FROM ReminderLog rl WHERE rl.reminderId = :reminderId AND rl.status = 'SENT' ORDER BY rl.sentTime DESC LIMIT 1")
    Optional<ReminderLog> findLatestSentLogByReminderId(@Param("reminderId") Long reminderId);

    @Query("SELECT rl FROM ReminderLog rl WHERE rl.reminderId IN :reminderIds AND rl.status = 'SENT' AND rl.userResponseStatus IS NOT NULL")
    List<ReminderLog> findUserResponsesByReminderIds(@Param("reminderIds") List<Long> reminderIds);
}