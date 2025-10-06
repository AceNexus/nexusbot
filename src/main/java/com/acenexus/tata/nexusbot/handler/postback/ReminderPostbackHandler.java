package com.acenexus.tata.nexusbot.handler.postback;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.reminder.ReminderLogService;
import com.acenexus.tata.nexusbot.reminder.ReminderService;
import com.acenexus.tata.nexusbot.reminder.ReminderStateManager;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_REMINDER_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_BOTH;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_LINE;
import static com.acenexus.tata.nexusbot.constants.Actions.DELETE_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.LIST_REMINDERS;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_COMPLETED;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_DAILY;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_ONCE;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_WEEKLY;
import static com.acenexus.tata.nexusbot.constants.Actions.TODAY_REMINDERS;

/**
 * 提醒功能 Handler - 處理提醒管理、重複類型、通知管道設定
 */
@Component
@Order(1) // 提醒功能優先順序最高
@RequiredArgsConstructor
public class ReminderPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderPostbackHandler.class);

    private final ReminderService reminderService;
    private final ReminderStateManager reminderStateManager;
    private final ReminderLogService reminderLogService;
    private final ReminderLogRepository reminderLogRepository;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(String action) {
        // 靜態動作
        if (REMINDER_MENU.equals(action) || ADD_REMINDER.equals(action) ||
                LIST_REMINDERS.equals(action) || TODAY_REMINDERS.equals(action) ||
                REPEAT_ONCE.equals(action) || REPEAT_DAILY.equals(action) || REPEAT_WEEKLY.equals(action) ||
                CHANNEL_LINE.equals(action) || CHANNEL_EMAIL.equals(action) || CHANNEL_BOTH.equals(action) ||
                CANCEL_REMINDER_INPUT.equals(action)) {
            return true;
        }

        // 動態動作（包含參數）
        return action.startsWith(DELETE_REMINDER) || action.startsWith(REMINDER_COMPLETED);
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("ReminderPostbackHandler handling action: {} for room: {}", action, roomId);

        // 處理動態動作
        if (action.startsWith(DELETE_REMINDER)) {
            return handleDeleteReminder(action, roomId);
        }
        if (action.startsWith(REMINDER_COMPLETED)) {
            return handleReminderCompleted(action, roomId);
        }

        // 處理靜態動作
        return switch (action) {
            case REMINDER_MENU -> {
                logger.debug("Showing reminder menu for room: {}", roomId);
                yield messageTemplateProvider.reminderMenu();
            }

            case ADD_REMINDER -> {
                reminderStateManager.startAddingReminder(roomId);
                logger.info("Started reminder creation flow for room: {}", roomId);
                yield messageTemplateProvider.reminderRepeatTypeMenu();
            }

            case LIST_REMINDERS -> {
                List<Reminder> reminders = reminderService.getActiveReminders(roomId);
                Map<Long, String> statuses = getConfirmationStatuses(reminders);
                logger.debug("Retrieved {} active reminders for room: {}", reminders.size(), roomId);
                yield messageTemplateProvider.reminderList(reminders, statuses);
            }

            case TODAY_REMINDERS -> {
                List<ReminderLogService.TodayReminderLog> todayLogs =
                        reminderLogService.getTodaysSentReminders(roomId);
                logger.debug("Retrieved {} today's reminder logs for room: {}", todayLogs.size(), roomId);
                yield messageTemplateProvider.todayReminderLogs(todayLogs);
            }

            case REPEAT_ONCE -> {
                reminderStateManager.setRepeatType(roomId, "ONCE");
                logger.debug("Set repeat type to ONCE for room: {}", roomId);
                yield messageTemplateProvider.reminderNotificationChannelMenu();
            }

            case REPEAT_DAILY -> {
                reminderStateManager.setRepeatType(roomId, "DAILY");
                logger.debug("Set repeat type to DAILY for room: {}", roomId);
                yield messageTemplateProvider.reminderNotificationChannelMenu();
            }

            case REPEAT_WEEKLY -> {
                reminderStateManager.setRepeatType(roomId, "WEEKLY");
                logger.debug("Set repeat type to WEEKLY for room: {}", roomId);
                yield messageTemplateProvider.reminderNotificationChannelMenu();
            }

            case CHANNEL_LINE -> {
                reminderStateManager.setNotificationChannel(roomId, "LINE");
                logger.debug("Set notification channel to LINE for room: {}", roomId);
                yield messageTemplateProvider.reminderInputMenu("time");
            }

            case CHANNEL_EMAIL -> {
                reminderStateManager.setNotificationChannel(roomId, "EMAIL");
                logger.debug("Set notification channel to EMAIL for room: {}", roomId);
                yield messageTemplateProvider.reminderInputMenu("time");
            }

            case CHANNEL_BOTH -> {
                reminderStateManager.setNotificationChannel(roomId, "BOTH");
                logger.debug("Set notification channel to BOTH for room: {}", roomId);
                yield messageTemplateProvider.reminderInputMenu("time");
            }

            case CANCEL_REMINDER_INPUT -> {
                reminderStateManager.clearState(roomId);
                logger.info("Cancelled reminder creation for room: {}", roomId);
                yield messageTemplateProvider.success("已取消新增提醒");
            }

            default -> {
                logger.warn("Unexpected action in ReminderPostbackHandler: {}", action);
                yield null;
            }
        };
    }

    private Message handleDeleteReminder(String data, String roomId) {
        try {
            String idStr = data.substring(data.indexOf("&id=") + 4);
            Long reminderId = Long.parseLong(idStr);

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

    private Message handleReminderCompleted(String data, String roomId) {
        try {
            String idStr = data.substring(data.indexOf("&id=") + 4);
            Long reminderId = Long.parseLong(idStr);

            logger.info("Reminder [{}] marked as completed by user in room [{}]", reminderId, roomId);

            updateReminderLogWithUserResponse(reminderId);
            return messageTemplateProvider.success("已記錄您已執行此提醒。");

        } catch (Exception e) {
            logger.error("Handle reminder completed error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("處理確認時發生錯誤");
        }
    }

    private void updateReminderLogWithUserResponse(Long reminderId) {
        try {
            // 查詢最新的發送記錄
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
            logger.error("Failed to update reminder log with user response for reminder [{}]: {}", reminderId, e.getMessage());
        }
    }

    private Map<Long, String> getConfirmationStatuses(List<Reminder> reminders) {
        Map<Long, String> statusMap = new HashMap<>();

        if (reminders.isEmpty()) {
            return statusMap;
        }

        try {
            for (Reminder reminder : reminders) {
                // 查詢最新的日誌記錄
                Optional<ReminderLog> latestLog = reminderLogRepository.findLatestByReminderId(reminder.getId());

                if (latestLog.isPresent()) {
                    ReminderLog log = latestLog.get();

                    // 檢查通知管道
                    String channel = reminder.getNotificationChannel() != null ? reminder.getNotificationChannel() : "LINE";

                    if ("EMAIL".equalsIgnoreCase(channel) || "BOTH".equalsIgnoreCase(channel)) {
                        // Email 通知或雙重通知：檢查 Email 確認狀態
                        statusMap.put(reminder.getId(), log.getConfirmedAt() != null ? "已確認" : "待確認");
                    } else {
                        // 僅 LINE 通知：檢查用戶回應狀態
                        String userResponse = log.getUserResponseStatus();
                        statusMap.put(reminder.getId(), "COMPLETED".equals(userResponse) ? "已執行" : "無回應");
                    }
                } else {
                    // 沒有日誌記錄，表示提醒尚未發送
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
    public int getPriority() {
        return 1;
    }
}
