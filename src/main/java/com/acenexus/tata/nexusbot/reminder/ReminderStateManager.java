package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.ReminderState;
import com.acenexus.tata.nexusbot.repository.ReminderStateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 提醒建立流程的狀態管理器。
 *
 * <p>方法依職責分為三類：
 * <ul>
 *   <li><b>store*()</b>：純欄位更新，不限定步驟、不推進流程</li>
 *   <li><b>transition*()</b> / 語意型方法：純步驟推進，不更新欄位</li>
 *   <li><b>get*()</b>：查詢</li>
 * </ul>
 *
 * <p>呼叫端先 store 所有需要的欄位，再呼叫對應的 transition 推進步驟，兩件事明確分開。
 */
@Component
@RequiredArgsConstructor
public class ReminderStateManager {

    private static final Logger logger = LoggerFactory.getLogger(ReminderStateManager.class);
    private final ReminderStateRepository reminderStateRepository;

    // ==================== 流程初始化 ====================

    /**
     * 開始新增提醒流程，建立初始狀態並進入 WAITING_FOR_REPEAT_TYPE。
     */
    @Transactional
    public void startAddingReminder(String roomId) {
        LocalDateTime now = LocalDateTime.now();
        ReminderState state = ReminderState.builder()
                .roomId(roomId)
                .step(ReminderState.Step.WAITING_FOR_REPEAT_TYPE.name())
                .repeatType("ONCE")
                .createdAt(now)
                .expiresAt(now.plusMinutes(30))
                .build();
        reminderStateRepository.save(state);
        logger.info("Started reminder creation flow for room: {}", roomId);
    }

    /**
     * 清除狀態（取消流程或完成建立後呼叫）。
     */
    @Transactional
    public void clearState(String roomId) {
        reminderStateRepository.deleteById(roomId);
        logger.info("Cleared reminder state for room: {}", roomId);
    }

    // ==================== store*：純欄位更新 ====================

    /**
     * 儲存重複類型（不推進步驟）。
     * 完成後應接著呼叫 {@link #transitionToNotificationChannel}。
     */
    @Transactional
    public void storeRepeatType(String roomId, String repeatType) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            state.setRepeatType(repeatType);
            reminderStateRepository.save(state);
            logger.debug("Stored repeat type for room {}: {}", roomId, repeatType);
        });
    }

    /**
     * 儲存通知管道（不推進步驟）。
     * 完成後應接著呼叫 {@link #transitionToTime}。
     */
    @Transactional
    public void storeNotificationChannel(String roomId, String notificationChannel) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            state.setNotificationChannel(notificationChannel);
            reminderStateRepository.save(state);
            logger.debug("Stored notification channel for room {}: {}", roomId, notificationChannel);
        });
    }

    /**
     * 儲存提醒時間（不推進步驟）。
     * 主流程完成後應接著呼叫 {@link #transitionToContent}；
     * 時區修改流程完成後應接著呼叫 {@link #transitionToTimezoneConfirmation}。
     */
    @Transactional
    public void storeTime(String roomId, LocalDateTime time) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            state.setReminderTime(time);
            reminderStateRepository.save(state);
            logger.debug("Stored reminder time for room {}: {}", roomId, time);
        });
    }

    /**
     * 儲存時區（不推進步驟）。
     */
    @Transactional
    public void storeTimezone(String roomId, String timezone) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            state.setTimezone(timezone);
            reminderStateRepository.save(state);
            logger.debug("Stored timezone for room {}: {}", roomId, timezone);
        });
    }

    /**
     * 儲存 Instant（不推進步驟）。
     */
    @Transactional
    public void storeInstant(String roomId, Instant instant) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            state.setReminderInstant(instant);
            reminderStateRepository.save(state);
            logger.debug("Stored instant for room {}: {}", roomId, instant);
        });
    }

    // ==================== transition*：純步驟推進 ====================

    /**
     * 推進至通知管道選擇步驟（WAITING_FOR_REPEAT_TYPE → WAITING_FOR_NOTIFICATION_CHANNEL）。
     */
    @Transactional
    public void transitionToNotificationChannel(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_REPEAT_TYPE,
                ReminderState.Step.WAITING_FOR_NOTIFICATION_CHANNEL,
                "transitionToNotificationChannel");
    }

    /**
     * 推進至時間輸入步驟（WAITING_FOR_NOTIFICATION_CHANNEL → WAITING_FOR_TIME）。
     */
    @Transactional
    public void transitionToTime(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_NOTIFICATION_CHANNEL,
                ReminderState.Step.WAITING_FOR_TIME,
                "transitionToTime");
    }

    /**
     * 推進至內容輸入步驟（WAITING_FOR_TIME → WAITING_FOR_CONTENT）。
     */
    @Transactional
    public void transitionToContent(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_TIME,
                ReminderState.Step.WAITING_FOR_CONTENT,
                "transitionToContent");
    }

    /**
     * 進入時區輸入步驟（WAITING_FOR_CONTENT → WAITING_FOR_TIMEZONE_INPUT）。
     */
    @Transactional
    public void transitionToTimezoneInput(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_CONTENT,
                ReminderState.Step.WAITING_FOR_TIMEZONE_INPUT,
                "transitionToTimezoneInput");
    }

    /**
     * 推進至時區確認步驟（WAITING_FOR_TIMEZONE_INPUT → WAITING_FOR_TIMEZONE_CONFIRMATION）。
     */
    @Transactional
    public void transitionToTimezoneConfirmation(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_TIMEZONE_INPUT,
                ReminderState.Step.WAITING_FOR_TIMEZONE_CONFIRMATION,
                "transitionToTimezoneConfirmation");
    }

    /**
     * 取消時區修改，從時區輸入或確認步驟返回 WAITING_FOR_CONTENT。
     */
    @Transactional
    public void cancelTimezoneChange(String roomId) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            String current = state.getStep();
            if (ReminderState.Step.WAITING_FOR_TIMEZONE_INPUT.name().equals(current)
                    || ReminderState.Step.WAITING_FOR_TIMEZONE_CONFIRMATION.name().equals(current)) {
                state.setStep(ReminderState.Step.WAITING_FOR_CONTENT.name());
                reminderStateRepository.save(state);
                logger.info("Cancelled timezone change for room: {}", roomId);
            }
        });
    }

    /**
     * 確認時區變更，從 WAITING_FOR_TIMEZONE_CONFIRMATION 返回 WAITING_FOR_CONTENT。
     */
    @Transactional
    public void confirmTimezoneChange(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_TIMEZONE_CONFIRMATION,
                ReminderState.Step.WAITING_FOR_CONTENT,
                "confirmTimezoneChange");
    }

    /**
     * 回到時間輸入步驟（WAITING_FOR_CONTENT → WAITING_FOR_TIME），用於修改提醒時間。
     */
    @Transactional
    public void startTimeChange(String roomId) {
        applyTransition(roomId,
                ReminderState.Step.WAITING_FOR_CONTENT,
                ReminderState.Step.WAITING_FOR_TIME,
                "startTimeChange");
    }

    // ==================== get*：查詢 ====================

    public Optional<ReminderState> getState(String roomId) {
        return reminderStateRepository.findByRoomIdAndNotExpired(roomId, LocalDateTime.now());
    }

    public ReminderState.Step getCurrentStep(String roomId) {
        return getState(roomId)
                .map(state -> ReminderState.Step.valueOf(state.getStep()))
                .orElse(null);
    }

    public LocalDateTime getTime(String roomId) {
        return getState(roomId).map(ReminderState::getReminderTime).orElse(null);
    }

    public String getTimezone(String roomId) {
        return getState(roomId).map(ReminderState::getTimezone).orElse(null);
    }

    public Instant getInstant(String roomId) {
        return getState(roomId).map(ReminderState::getReminderInstant).orElse(null);
    }

    public String getRepeatType(String roomId) {
        return getState(roomId).map(ReminderState::getRepeatType).orElse("ONCE");
    }

    public String getNotificationChannel(String roomId) {
        return getState(roomId).map(ReminderState::getNotificationChannel).orElse("LINE");
    }

    // ==================== 內部工具 ====================

    /**
     * 通用步驟推進，確認當前步驟符合預期才執行。
     */
    private void applyTransition(String roomId, ReminderState.Step expectedFrom,
                                 ReminderState.Step to, String caller) {
        reminderStateRepository.findById(roomId).ifPresent(state -> {
            if (expectedFrom.name().equals(state.getStep())) {
                state.setStep(to.name());
                reminderStateRepository.save(state);
                logger.info("[{}] room {} : {} → {}", caller, roomId, expectedFrom, to);
            } else {
                logger.warn("[{}] room {} skipped: expected step {} but was {}", caller, roomId, expectedFrom, state.getStep());
            }
        });
    }
}
