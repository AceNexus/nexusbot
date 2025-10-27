package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.entity.ReminderState;
import com.acenexus.tata.nexusbot.reminder.ReminderLogService;
import com.acenexus.tata.nexusbot.reminder.ReminderService;
import com.acenexus.tata.nexusbot.reminder.ReminderStateManager;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.acenexus.tata.nexusbot.util.AnalyzerUtil;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

/**
 * 提醒功能 Facade 實作
 */
@Service
@RequiredArgsConstructor
public class ReminderFacadeImpl implements ReminderFacade {

    private static final Logger logger = LoggerFactory.getLogger(ReminderFacadeImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = STANDARD_TIME;

    private final ReminderService reminderService;
    private final ReminderStateManager reminderStateManager;
    private final ReminderLogService reminderLogService;
    private final ReminderLogRepository reminderLogRepository;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public Message showMenu() {
        return messageTemplateProvider.reminderMenu();
    }

    @Override
    public Message startCreation(String roomId) {
        reminderStateManager.startAddingReminder(roomId);
        logger.info("Started reminder creation flow for room: {}", roomId);
        return messageTemplateProvider.reminderRepeatTypeMenu();
    }

    @Override
    public Message listActive(String roomId) {
        List<Reminder> reminders = reminderService.getActiveReminders(roomId);
        Map<Long, String> statuses = getConfirmationStatuses(reminders);
        logger.debug("Retrieved {} active reminders for room: {}", reminders.size(), roomId);
        return messageTemplateProvider.reminderList(reminders, statuses);
    }

    @Override
    public Message showTodayLogs(String roomId) {
        List<ReminderLogService.TodayReminderLog> todayLogs = reminderLogService.getTodaysSentReminders(roomId);
        logger.debug("Retrieved {} today's reminder logs for room: {}", todayLogs.size(), roomId);
        return messageTemplateProvider.todayReminderLogs(todayLogs);
    }

    @Override
    public Message deleteReminder(Long reminderId, String roomId) {
        try {
            boolean success = reminderService.deleteReminder(reminderId, roomId);
            if (success) {
                logger.info("Deleted reminder {} for room: {}", reminderId, roomId);
                return messageTemplateProvider.success("提醒已刪除");
            } else {
                logger.warn("Failed to delete reminder {} for room: {}", reminderId, roomId);
                return messageTemplateProvider.error("刪除失敗");
            }
        } catch (Exception e) {
            logger.error("Delete reminder error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("刪除提醒時發生錯誤");
        }
    }

    @Override
    public void confirmReminder(Long reminderId, String roomId) {
        logger.info("Reminder [{}] marked as completed by user in room [{}]", reminderId, roomId);
        updateReminderLogWithUserResponse(reminderId);
    }

    @Override
    public void sendNotification(Reminder reminder, String enhancedContent) {
        // 此方法將在 Week 3 通知模組整合時實作
        logger.debug("Sending notification for reminder: {}", reminder.getId());
    }

    /**
     * 處理提醒互動流程
     */
    public Message handleInteraction(String roomId, String messageText, String replyToken) {
        ReminderState.Step currentStep = reminderStateManager.getCurrentStep(roomId);
        if (currentStep == null) {
            return null; // 用戶不在提醒流程中
        }

        try {
            return switch (currentStep) {
                case WAITING_FOR_TIME -> handleTimeInput(roomId, messageText);
                case WAITING_FOR_CONTENT -> handleContentInput(roomId, messageText);
                default -> null;
            };
        } catch (Exception e) {
            logger.error("Error processing reminder interaction: {}", e.getMessage());
            reminderStateManager.clearState(roomId);
            return messageTemplateProvider.reminderInputError("系統錯誤", "處理提醒時發生錯誤");
        }
    }

    private Message handleTimeInput(String roomId, String input) {
        input = input.trim();
        LocalDateTime reminderTime;

        // 先嘗試標準格式，失敗則使用 AI 解析
        if (input.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
            try {
                reminderTime = LocalDateTime.parse(input, TIME_FORMATTER)
                        .withSecond(0).withNano(0);
            } catch (Exception e) {
                reminderTime = AnalyzerUtil.parseTime(input);
            }
        } else {
            reminderTime = AnalyzerUtil.parseTime(input);
        }

        if (reminderTime == null) {
            return messageTemplateProvider.reminderInputError(input, "無法解析時間格式");
        }

        if (reminderTime.isBefore(LocalDateTime.now())) {
            return messageTemplateProvider.reminderInputError(
                    input,
                    "時間必須是未來\n" + reminderTime.format(TIME_FORMATTER)
            );
        }

        // 儲存時間並進入下一步
        reminderStateManager.setTime(roomId, reminderTime);
        return messageTemplateProvider.reminderInputMenu("content", reminderTime.format(TIME_FORMATTER));
    }

    private Message handleContentInput(String roomId, String content) {
        content = content.trim();

        LocalDateTime reminderTime = reminderStateManager.getTime(roomId);
        String repeatType = reminderStateManager.getRepeatType(roomId);
        String notificationChannel = reminderStateManager.getNotificationChannel(roomId);

        // 創建提醒
        reminderService.createReminder(roomId, content, reminderTime, repeatType, roomId, notificationChannel);

        // 清除狀態
        reminderStateManager.clearState(roomId);

        String repeatTypeText = switch (repeatType) {
            case "DAILY" -> "每日重複";
            case "WEEKLY" -> "每週重複";
            default -> "僅一次";
        };

        logger.info("Reminder created for room {}: {} at {}", roomId, content, reminderTime);
        return messageTemplateProvider.reminderCreatedSuccess(
                reminderTime.format(TIME_FORMATTER),
                repeatTypeText,
                content
        );
    }

    private void updateReminderLogWithUserResponse(Long reminderId) {
        try {
            Optional<ReminderLog> logOptional = reminderLogRepository.findLatestSentLogByReminderId(reminderId);

            if (logOptional.isPresent()) {
                ReminderLog log = logOptional.get();
                log.setUserResponseTime(LocalDateTime.now());
                log.setUserResponseStatus("COMPLETED");
                reminderLogRepository.save(log);

                logger.info("Updated reminder log [{}] with user response: COMPLETED", log.getId());
            } else {
                logger.warn("No sent log found for reminder [{}], cannot record user response", reminderId);
            }
        } catch (Exception e) {
            logger.error("Failed to update reminder log with user response for reminder [{}]: {}",
                    reminderId, e.getMessage());
        }
    }

    private Map<Long, String> getConfirmationStatuses(List<Reminder> reminders) {
        Map<Long, String> statusMap = new HashMap<>();

        if (reminders.isEmpty()) {
            return statusMap;
        }

        try {
            for (Reminder reminder : reminders) {
                Optional<ReminderLog> latestLog = reminderLogRepository.findLatestByReminderId(reminder.getId());

                if (latestLog.isPresent()) {
                    ReminderLog log = latestLog.get();
                    String channel = reminder.getNotificationChannel() != null ? reminder.getNotificationChannel() : "LINE";

                    if ("EMAIL".equalsIgnoreCase(channel) || "BOTH".equalsIgnoreCase(channel)) {
                        statusMap.put(reminder.getId(), log.getConfirmedAt() != null ? "已確認" : "待確認");
                    } else {
                        String userResponse = log.getUserResponseStatus();
                        statusMap.put(reminder.getId(), "COMPLETED".equals(userResponse) ? "已執行" : "無回應");
                    }
                } else {
                    statusMap.put(reminder.getId(), "未發送");
                }
            }

            logger.debug("Retrieved confirmation statuses for {} reminders", reminders.size());
        } catch (Exception e) {
            logger.error("Failed to retrieve confirmation statuses: {}", e.getMessage());
        }

        return statusMap;
    }

    @Override
    public boolean isInReminderFlow(String roomId) {
        ReminderState.Step currentStep = reminderStateManager.getCurrentStep(roomId);
        return currentStep != null;
    }
}
