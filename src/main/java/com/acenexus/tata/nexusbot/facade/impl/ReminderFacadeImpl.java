package com.acenexus.tata.nexusbot.facade.impl;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomAccessor;
import com.acenexus.tata.nexusbot.email.EmailInputStateService;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.entity.ReminderState;
import com.acenexus.tata.nexusbot.facade.ReminderFacade;
import com.acenexus.tata.nexusbot.reminder.ReminderLogService;
import com.acenexus.tata.nexusbot.reminder.ReminderService;
import com.acenexus.tata.nexusbot.reminder.ReminderStateManager;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.acenexus.tata.nexusbot.timezone.TimezoneInputStateService;
import com.acenexus.tata.nexusbot.util.AnalyzerUtil;
import com.acenexus.tata.nexusbot.util.ParsedTimeResult;
import com.acenexus.tata.nexusbot.util.TimezoneValidator;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final MessageTemplateProvider messageTemplateProvider;
    private final ChatRoomAccessor chatRoomAccessor;
    private final EmailInputStateService emailInputStateService;
    private final TimezoneInputStateService timezoneInputStateService;
    private final AnalyzerUtil analyzerUtil;

    @Override
    public Message showMenu() {
        return messageTemplateProvider.reminderMenu();
    }

    @Override
    public Message startCreation(String roomId) {
        // 互斥保護：清除其他流程狀態
        emailInputStateService.clearWaitingForEmailInput(roomId);
        timezoneInputStateService.clearWaitingForTimezoneInput(roomId);

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
    public Message confirmReminder(Long reminderId, String roomId) {
        logger.info("Reminder [{}] marked as completed by user in room [{}]", reminderId, roomId);
        boolean success = reminderLogService.updateWithUserResponse(reminderId);

        if (success) {
            logger.info("Updated reminder log with user response for reminder: {}", reminderId);
            return messageTemplateProvider.success("已記錄您已執行此提醒。");
        } else {
            logger.warn("No sent log found for reminder [{}], cannot record user response", reminderId);
            return messageTemplateProvider.success("已記錄您已執行此提醒。");
        }
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
                case WAITING_FOR_TIMEZONE_INPUT -> handleTimezoneInput(roomId, messageText);
                case WAITING_FOR_TIMEZONE_CONFIRMATION -> {
                    // 理論上用戶不應該在確認步驟輸入文字，應該點擊按鈕
                    logger.warn("User entered text in WAITING_FOR_TIMEZONE_CONFIRMATION step for room: {}", roomId);
                    LocalDateTime reminderTime = reminderStateManager.getTime(roomId);
                    String timezone = reminderStateManager.getTimezone(roomId);
                    String timezoneDisplay = TimezoneValidator.getDisplayName(timezone);
                    String originalInput = reminderStateManager.getTimezone(roomId);
                    yield messageTemplateProvider.timezoneConfirmationMenu(timezone, timezoneDisplay, reminderTime.format(TIME_FORMATTER), originalInput);
                }
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

        // 1. 取得時區（優先使用 ReminderState 中已設定的時區，否則使用 ChatRoom 預設時區）
        String stateTimezone = reminderStateManager.getTimezone(roomId);
        String defaultTimezone;
        if (stateTimezone != null) {
            // 如果 ReminderState 中已有時區設定（例如之前修改過時區），使用該時區
            defaultTimezone = stateTimezone;
            logger.debug("Using timezone from ReminderState for room {}: {}", roomId, stateTimezone);
        } else {
            // 否則使用 ChatRoom 的預設時區
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, ChatRoom.RoomType.USER);
            defaultTimezone = chatRoom.getTimezone();
            logger.debug("Using timezone from ChatRoom for room {}: {}", roomId, defaultTimezone);
        }

        // 2. 使用 AI 解析時間與時區
        ParsedTimeResult parseResult = analyzerUtil.parseTimeWithTimezone(input, defaultTimezone);

        if (parseResult == null) {
            return messageTemplateProvider.reminderInputError(input, "無法解析時間格式");
        }

        // 3. 決定最終使用的時區
        String finalTimezone = parseResult.hasTimezone()
                ? parseResult.getTimezone()      // 使用者明確指定的時區
                : defaultTimezone;                // 使用 ChatRoom 預設時區

        // 4. 轉換為 ZonedDateTime 和 Instant
        LocalDateTime localTime = parseResult.getDateTime();
        ZonedDateTime zonedTime = localTime.atZone(ZoneId.of(finalTimezone));
        Instant instant = zonedTime.toInstant();

        // 5. 驗證時間必須是未來
        if (instant.isBefore(Instant.now())) {
            String timezoneDisplay = TimezoneValidator.getDisplayName(finalTimezone);
            return messageTemplateProvider.reminderInputError(input, String.format("時間必須是未來\n%s (%s)", localTime.format(TIME_FORMATTER), timezoneDisplay));
        }

        // 6. 儲存時間、時區、Instant，再推進步驟
        reminderStateManager.storeTime(roomId, localTime);
        reminderStateManager.storeTimezone(roomId, finalTimezone);
        reminderStateManager.storeInstant(roomId, instant);
        reminderStateManager.transitionToContent(roomId);

        // 7. 顯示確認畫面（含時區）
        String timezoneDisplay = TimezoneValidator.getDisplayName(finalTimezone);
        logger.info("Parsed time for room {}: {} at timezone {} (instant: {})", roomId, localTime, finalTimezone, instant);

        return messageTemplateProvider.reminderInputMenu(
                "content",
                localTime.format(TIME_FORMATTER),
                timezoneDisplay
        );
    }

    private Message handleContentInput(String roomId, String content) {
        content = content.trim();

        // 優化：一次查詢獲取所有狀態資料，避免多次查詢
        Optional<ReminderState> stateOpt = reminderStateManager.getState(roomId);
        if (stateOpt.isEmpty()) {
            logger.warn("No reminder state found for room: {}", roomId);
            return messageTemplateProvider.error("提醒狀態已過期，請重新建立提醒");
        }

        ReminderState state = stateOpt.get();
        LocalDateTime reminderTime = state.getReminderTime();
        String timezone = state.getTimezone();
        Instant instant = state.getReminderInstant();
        String repeatType = state.getRepeatType();
        String notificationChannel = state.getNotificationChannel();

        // 創建提醒（含時區與 Instant）
        Reminder reminder = reminderService.createReminder(roomId, content, reminderTime, timezone, instant, repeatType, roomId, notificationChannel);
        logger.info("Reminder created: {}", reminder.toString());

        // 清除狀態
        reminderStateManager.clearState(roomId);

        String repeatTypeText = switch (repeatType) {
            case "DAILY" -> "每日重複";
            case "WEEKLY" -> "每週重複";
            default -> "僅一次";
        };

        String timezoneDisplay = TimezoneValidator.getDisplayName(timezone);
        logger.info("Reminder created for room {}: {} at {} ({})", roomId, content, reminderTime, timezone);

        return messageTemplateProvider.reminderCreatedSuccess(reminderTime.format(TIME_FORMATTER), repeatTypeText, content, timezoneDisplay);
    }

    private Map<Long, String> getConfirmationStatuses(List<Reminder> reminders) {
        Map<Long, String> statusMap = new HashMap<>();

        if (reminders.isEmpty()) {
            return statusMap;
        }

        try {
            for (Reminder reminder : reminders) {
                Optional<ReminderLog> latestLog = reminderLogService.findLatestByReminderId(reminder.getId());

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

    // ==================== 重複類型選擇 ====================

    @Override
    public Message setRepeatTypeOnce(String roomId) {
        reminderStateManager.storeRepeatType(roomId, "ONCE");
        reminderStateManager.transitionToNotificationChannel(roomId);
        logger.debug("Set repeat type to ONCE for room: {}", roomId);
        return messageTemplateProvider.reminderNotificationChannelMenu();
    }

    @Override
    public Message setRepeatTypeDaily(String roomId) {
        reminderStateManager.storeRepeatType(roomId, "DAILY");
        reminderStateManager.transitionToNotificationChannel(roomId);
        logger.debug("Set repeat type to DAILY for room: {}", roomId);
        return messageTemplateProvider.reminderNotificationChannelMenu();
    }

    @Override
    public Message setRepeatTypeWeekly(String roomId) {
        reminderStateManager.storeRepeatType(roomId, "WEEKLY");
        reminderStateManager.transitionToNotificationChannel(roomId);
        logger.debug("Set repeat type to WEEKLY for room: {}", roomId);
        return messageTemplateProvider.reminderNotificationChannelMenu();
    }

    // ==================== 通知管道選擇 ====================

    @Override
    public Message setNotificationChannelLine(String roomId) {
        reminderStateManager.storeNotificationChannel(roomId, "LINE");
        reminderStateManager.transitionToTime(roomId);
        logger.debug("Set notification channel to LINE for room: {}", roomId);
        return messageTemplateProvider.reminderInputMenu("time", "", "");
    }

    @Override
    public Message setNotificationChannelEmail(String roomId) {
        reminderStateManager.storeNotificationChannel(roomId, "EMAIL");
        reminderStateManager.transitionToTime(roomId);
        logger.debug("Set notification channel to EMAIL for room: {}", roomId);
        return messageTemplateProvider.reminderInputMenu("time", "", "");
    }

    @Override
    public Message setNotificationChannelBoth(String roomId) {
        reminderStateManager.storeNotificationChannel(roomId, "BOTH");
        reminderStateManager.transitionToTime(roomId);
        logger.debug("Set notification channel to BOTH for room: {}", roomId);
        return messageTemplateProvider.reminderInputMenu("time", "", "");
    }

    // ==================== 取消操作 ====================

    @Override
    public Message cancelCreation(String roomId) {
        reminderStateManager.clearState(roomId);
        logger.info("Cancelled reminder creation for room: {}", roomId);
        return messageTemplateProvider.success("已取消新增提醒");
    }

    // ==================== 時區修改 ====================

    @Override
    public Message startTimezoneChange(String roomId) {
        reminderStateManager.transitionToTimezoneInput(roomId);
        String currentTimezone = reminderStateManager.getTimezone(roomId);
        String timezoneDisplay = TimezoneValidator.getDisplayName(currentTimezone);
        logger.info("Started timezone change for room: {}", roomId);
        return messageTemplateProvider.timezoneInputMenu(timezoneDisplay);
    }

    @Override
    public Message cancelTimezoneChange(String roomId) {
        reminderStateManager.cancelTimezoneChange(roomId);

        // 取得當前時間和時區資訊顯示確認畫面
        LocalDateTime reminderTime = reminderStateManager.getTime(roomId);
        String timezone = reminderStateManager.getTimezone(roomId);
        String timezoneDisplay = TimezoneValidator.getDisplayName(timezone);

        logger.info("Cancelled timezone change for room: {}", roomId);
        return messageTemplateProvider.reminderInputMenu("content", reminderTime.format(TIME_FORMATTER), timezoneDisplay);
    }

    @Override
    public Message confirmTimezoneChange(String roomId) {
        // 確認時區變更並將狀態改回等待內容步驟
        reminderStateManager.confirmTimezoneChange(roomId);

        LocalDateTime reminderTime = reminderStateManager.getTime(roomId);
        String timezone = reminderStateManager.getTimezone(roomId);
        String timezoneDisplay = TimezoneValidator.getDisplayName(timezone);

        logger.info("Confirmed timezone change for room: {}", roomId);
        return messageTemplateProvider.reminderInputMenu("content", reminderTime.format(TIME_FORMATTER), timezoneDisplay);
    }

    /**
     * 處理使用者輸入的時區
     */
    private Message handleTimezoneInput(String roomId, String input) {
        input = input.trim();

        // 1. 嘗試解析時區
        String resolvedTimezone = TimezoneValidator.resolveTimezone(input);

        if (resolvedTimezone == null) {
            logger.warn("Unable to resolve timezone input: {}", input);
            return messageTemplateProvider.timezoneInputError(input);
        }

        // 2. 取得原始 Instant（保持絕對時間點不變）
        Instant originalInstant = reminderStateManager.getInstant(roomId);

        // 3. 根據新時區計算對應的本地時間
        ZonedDateTime newZonedTime = originalInstant.atZone(ZoneId.of(resolvedTimezone));
        LocalDateTime newLocalTime = newZonedTime.toLocalDateTime();

        // 4. 先更新時區與換算後的本地時間，再推進步驟至確認（Instant 保持不變）
        reminderStateManager.storeTimezone(roomId, resolvedTimezone);
        reminderStateManager.storeTime(roomId, newLocalTime);
        reminderStateManager.transitionToTimezoneConfirmation(roomId);

        // 5. 顯示確認畫面
        String timezoneDisplay = TimezoneValidator.getDisplayName(resolvedTimezone);
        String newReminderTimeDisplay = newLocalTime.format(TIME_FORMATTER);

        logger.info("Parsed timezone for room {}: {} -> {}", roomId, input, resolvedTimezone);

        return messageTemplateProvider.timezoneConfirmationMenu(resolvedTimezone, timezoneDisplay, newReminderTimeDisplay, input);
    }

    @Override
    public Message startTimeChange(String roomId) {
        reminderStateManager.startTimeChange(roomId);
        logger.info("Started time change for room: {}", roomId);
        return messageTemplateProvider.reminderInputMenu("time", "", "");
    }
}
