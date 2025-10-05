package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;

    @Override
    public Reminder createReminder(String roomId, String content, LocalDateTime reminderTime,
                                   String repeatType, String createdBy) {
        return createReminder(roomId, content, reminderTime, repeatType, createdBy, "LINE");
    }

    @Override
    public Reminder createReminder(String roomId, String content, LocalDateTime reminderTime,
                                   String repeatType, String createdBy, String notificationChannel) {
        Reminder reminder = Reminder.builder()
                .roomId(roomId)
                .content(content)
                .reminderTime(reminderTime)
                .repeatType(repeatType != null ? repeatType : "ONCE")
                .notificationChannel(notificationChannel != null ? notificationChannel : "LINE")
                .status("ACTIVE")
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        return reminderRepository.save(reminder);
    }

    @Override
    public List<Reminder> getActiveReminders(String roomId) {
        return reminderRepository.findActiveRemindersByRoomId(roomId);
    }

    @Override
    public boolean deleteReminder(Long reminderId, String roomId) {
        try {
            var reminder = reminderRepository.findById(reminderId);
            if (reminder.isPresent() && reminder.get().getRoomId().equals(roomId)) {
                reminderRepository.deleteById(reminderId);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}