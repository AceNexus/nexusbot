package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.dto.ConfirmationResult;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 優化版本：使用批量查詢避免 N+1 問題
     */
    public List<TodayReminderLog> getTodaysSentReminders(String roomId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 1. 查詢今日所有已發送的 logs
        List<ReminderLog> logs = reminderLogRepository.findTodaysSentLogs(roomId, startOfDay, endOfDay);

        if (logs.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 收集所有不重複的 reminderId
        Set<Long> reminderIds = logs.stream()
                .map(ReminderLog::getReminderId)
                .collect(Collectors.toSet());

        // 3. 批量查詢所有相關的 Reminder（只需 1 次查詢）
        List<Reminder> reminders = reminderRepository.findAllById(reminderIds);

        // 4. 建立 reminderMap 與 logsByReminderId
        Map<Long, Reminder> reminderMap = reminders.stream()
                .collect(Collectors.toMap(Reminder::getId, r -> r));

        Map<Long, List<ReminderLog>> logsByReminderId = logs.stream()
                .collect(Collectors.groupingBy(ReminderLog::getReminderId));

        // 5. 每個 reminderId 只處理一次，直接取對應的 logs 判斷確認狀態
        List<TodayReminderLog> result = new ArrayList<>();

        for (Map.Entry<Long, List<ReminderLog>> entry : logsByReminderId.entrySet()) {
            Reminder reminder = reminderMap.get(entry.getKey());
            if (reminder == null) {
                continue;
            }

            List<ReminderLog> reminderLogs = entry.getValue();
            LocalDateTime sentTime = reminderLogs.get(0).getSentTime();

            boolean isConfirmed = reminderLogs.stream().anyMatch(l ->
                    ("LINE".equalsIgnoreCase(l.getDeliveryMethod()) && "COMPLETED".equals(l.getUserResponseStatus())) ||
                            ("EMAIL".equalsIgnoreCase(l.getDeliveryMethod()) && l.getConfirmedAt() != null)
            );

            result.add(new TodayReminderLog(reminder.getId(), reminder.getContent(), sentTime, reminder.getTimezone() != null ? reminder.getTimezone() : "Asia/Taipei", isConfirmed));
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

    /**
     * 透過 Token 確認提醒
     *
     * @param token 確認 Token
     * @return 確認結果
     */
    public ConfirmationResult confirmByToken(String token) {
        try {
            Optional<ReminderLog> optionalLog = reminderLogRepository.findByConfirmationToken(token);

            if (optionalLog.isEmpty()) {
                return new ConfirmationResult(false, "無效的確認連結", "此連結可能已過期或不存在", false);
            }

            ReminderLog log = optionalLog.get();

            if (log.getConfirmedAt() != null) {
                String detail = "確認時間: " + log.getConfirmedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
                return new ConfirmationResult(true, "此提醒已確認", detail, true);
            }

            log.setConfirmedAt(LocalDateTime.now());
            reminderLogRepository.save(log);

            return new ConfirmationResult(true, "提醒確認成功", "您可以返回 LINE 查看提醒狀態", false);
        } catch (Exception e) {
            return new ConfirmationResult(false, "確認失敗", "系統發生錯誤，請稍後再試", false);
        }
    }
}
