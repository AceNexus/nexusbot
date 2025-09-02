package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}