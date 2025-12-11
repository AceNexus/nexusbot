package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 提醒日誌服務
 * 提供今日提醒記錄查詢等功能
 */
@Service
@RequiredArgsConstructor
public class ReminderLogService {

    private final ReminderLogRepository reminderLogRepository;
    private final ReminderRepository reminderRepository;

    /**
     * 今日提醒記錄 DTO
     * 合併同一個提醒的多條記錄（如雙重通知）
     */
    public record TodayReminderLog(
            Long reminderId,
            String content,
            LocalDateTime sentTime,
            String timezone,     // 提醒時區
            boolean isConfirmed  // 任一方式確認即為 true
    ) {
    }

    /**
     * 查詢今日已發送的提醒記錄（合併同一提醒的多條 log）
     */
    public List<TodayReminderLog> getTodaysSentReminders(String roomId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<ReminderLog> logs = reminderLogRepository.findTodaysSentLogs(roomId, startOfDay, endOfDay);
        List<TodayReminderLog> result = new ArrayList<>();
        java.util.Set<Long> processedReminderIds = new java.util.HashSet<>();

        for (ReminderLog log : logs) {
            // 如果這個提醒已經處理過，跳過
            if (processedReminderIds.contains(log.getReminderId())) {
                continue;
            }

            // 查詢對應的 Reminder
            Optional<Reminder> reminderOpt = reminderRepository.findById(log.getReminderId());

            if (reminderOpt.isPresent()) {
                Reminder reminder = reminderOpt.get();

                // 檢查是否已確認
                boolean isConfirmed = false;
                LocalDateTime sentTime = log.getSentTime();

                for (ReminderLog l : logs) {
                    if (l.getReminderId().equals(reminder.getId())) {
                        // LINE 確認
                        if ("LINE".equalsIgnoreCase(l.getDeliveryMethod())
                                && "COMPLETED".equals(l.getUserResponseStatus())) {
                            isConfirmed = true;
                            break;
                        }
                        // Email 確認
                        if ("EMAIL".equalsIgnoreCase(l.getDeliveryMethod())
                                && l.getConfirmedAt() != null) {
                            isConfirmed = true;
                            break;
                        }
                    }
                }

                result.add(new TodayReminderLog(
                        reminder.getId(),
                        reminder.getContent(),
                        sentTime,
                        reminder.getTimezone() != null ? reminder.getTimezone() : "Asia/Taipei",
                        isConfirmed
                ));

                processedReminderIds.add(reminder.getId());
            }
        }

        return result;
    }

    /**
     * 查找提醒的最新日誌（不限狀態）
     *
     * @param reminderId 提醒 ID
     * @return 最新日誌（如果存在）
     */
    public Optional<ReminderLog> findLatestByReminderId(Long reminderId) {
        return reminderLogRepository.findLatestByReminderId(reminderId);
    }

    /**
     * 更新提醒日誌的用戶回應
     *
     * @param reminderId 提醒 ID
     * @return 是否更新成功
     */
    public boolean updateWithUserResponse(Long reminderId) {
        try {
            Optional<ReminderLog> logOptional = reminderLogRepository.findLatestSentLogByReminderId(reminderId);

            if (logOptional.isPresent()) {
                ReminderLog log = logOptional.get();
                log.setUserResponseTime(LocalDateTime.now());
                log.setUserResponseStatus("COMPLETED");
                reminderLogRepository.save(log);

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
