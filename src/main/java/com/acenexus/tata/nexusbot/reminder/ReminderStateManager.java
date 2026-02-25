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

@Component
@RequiredArgsConstructor
public class ReminderStateManager {

    private static final Logger logger = LoggerFactory.getLogger(ReminderStateManager.class);
    private final ReminderStateRepository reminderStateRepository;

    /**
     * 開始新增提醒流程
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
     * 設置時間並進入下一步
     */
    @Transactional
    public void setTime(String roomId, LocalDateTime time) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            if (ReminderState.Step.WAITING_FOR_TIME.name().equals(state.getStep())) {
                state.setReminderTime(time);
                state.setStep(ReminderState.Step.WAITING_FOR_CONTENT.name());
                reminderStateRepository.save(state);
                logger.info("Set reminder time for room {}: {}", roomId, time);
            }
        }
    }

    /**
     * 獲取提醒狀態
     */
    public Optional<ReminderState> getState(String roomId) {
        return reminderStateRepository.findById(roomId);
    }

    /**
     * 獲取當前步驟
     */
    public ReminderState.Step getCurrentStep(String roomId) {
        return getState(roomId)
                .map(state -> ReminderState.Step.valueOf(state.getStep()))
                .orElse(null);
    }

    /**
     * 獲取暫存的時間
     */
    public LocalDateTime getTime(String roomId) {
        return getState(roomId)
                .map(ReminderState::getReminderTime)
                .orElse(null);
    }

    /**
     * 清除狀態
     */
    @Transactional
    public void clearState(String roomId) {
        reminderStateRepository.deleteById(roomId);
        logger.info("Cleared reminder state for room: {}", roomId);
    }

    /**
     * 設置重複類型並進入下一步（通知管道選擇）
     */
    @Transactional
    public void setRepeatType(String roomId, String repeatType) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            if (ReminderState.Step.WAITING_FOR_REPEAT_TYPE.name().equals(state.getStep())) {
                state.setRepeatType(repeatType);
                state.setStep(ReminderState.Step.WAITING_FOR_NOTIFICATION_CHANNEL.name());
                reminderStateRepository.save(state);
                logger.info("Set repeat type for room {}: {}", roomId, repeatType);
            }
        }
    }

    /**
     * 設置通知管道並進入下一步（時間輸入）
     */
    @Transactional
    public void setNotificationChannel(String roomId, String notificationChannel) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            if (ReminderState.Step.WAITING_FOR_NOTIFICATION_CHANNEL.name().equals(state.getStep())) {
                state.setNotificationChannel(notificationChannel);
                state.setStep(ReminderState.Step.WAITING_FOR_TIME.name());
                reminderStateRepository.save(state);
                logger.info("Set notification channel for room {}: {}", roomId, notificationChannel);
            }
        }
    }

    /**
     * 獲取通知管道
     */
    public String getNotificationChannel(String roomId) {
        return getState(roomId)
                .map(ReminderState::getNotificationChannel)
                .orElse("LINE");
    }

    /**
     * 獲取重複類型
     */
    public String getRepeatType(String roomId) {
        return getState(roomId)
                .map(ReminderState::getRepeatType)
                .orElse("ONCE");
    }

    /**
     * 設置時區
     */
    @Transactional
    public void setTimezone(String roomId, String timezone) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            state.setTimezone(timezone);
            reminderStateRepository.save(state);
            logger.debug("Set timezone for room {}: {}", roomId, timezone);
        }
    }

    /**
     * 獲取時區
     */
    public String getTimezone(String roomId) {
        return getState(roomId)
                .map(ReminderState::getTimezone)
                .orElse(null);
    }

    /**
     * 設置 Instant
     */
    @Transactional
    public void setInstant(String roomId, Instant instant) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            state.setReminderInstant(instant);
            reminderStateRepository.save(state);
            logger.debug("Set instant for room {}: {}", roomId, instant);
        }
    }

    /**
     * 獲取 Instant
     */
    public Instant getInstant(String roomId) {
        return getState(roomId)
                .map(ReminderState::getReminderInstant)
                .orElse(null);
    }

    /**
     * 進入時區修改流程（等待使用者輸入時區）
     */
    @Transactional
    public void startTimezoneChange(String roomId) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            // 從等待內容步驟進入時區輸入步驟
            if (ReminderState.Step.WAITING_FOR_CONTENT.name().equals(state.getStep())) {
                state.setStep(ReminderState.Step.WAITING_FOR_TIMEZONE_INPUT.name());
                reminderStateRepository.save(state);
                logger.info("Started timezone change flow for room: {}", roomId);
            }
        }
    }

    /**
     * 進入時區確認流程
     */
    @Transactional
    public void moveToTimezoneConfirmation(String roomId) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            if (ReminderState.Step.WAITING_FOR_TIMEZONE_INPUT.name().equals(state.getStep())) {
                state.setStep(ReminderState.Step.WAITING_FOR_TIMEZONE_CONFIRMATION.name());
                reminderStateRepository.save(state);
                logger.debug("Moved to timezone confirmation for room: {}", roomId);
            }
        }
    }

    /**
     * 取消時區修改，返回等待內容步驟
     */
    @Transactional
    public void cancelTimezoneChange(String roomId) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            String currentStep = state.getStep();

            // 從時區輸入或確認步驟返回等待內容步驟
            if (ReminderState.Step.WAITING_FOR_TIMEZONE_INPUT.name().equals(currentStep) ||
                    ReminderState.Step.WAITING_FOR_TIMEZONE_CONFIRMATION.name().equals(currentStep)) {
                state.setStep(ReminderState.Step.WAITING_FOR_CONTENT.name());
                reminderStateRepository.save(state);
                logger.info("Cancelled timezone change for room: {}", roomId);
            }
        }
    }

    /**
     * 確認時區變更並返回等待內容步驟
     */
    @Transactional
    public void confirmTimezoneChangeAndReturnToContent(String roomId) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            if (ReminderState.Step.WAITING_FOR_TIMEZONE_CONFIRMATION.name().equals(state.getStep())) {
                state.setStep(ReminderState.Step.WAITING_FOR_CONTENT.name());
                reminderStateRepository.save(state);
                logger.info("Confirmed timezone change and returned to content input for room: {}", roomId);
            }
        }
    }

    /**
     * 開始修改時間流程（從等待內容步驟返回等待時間步驟）
     */
    @Transactional
    public void startTimeChange(String roomId) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            // 從等待內容步驟返回等待時間步驟
            if (ReminderState.Step.WAITING_FOR_CONTENT.name().equals(state.getStep())) {
                state.setStep(ReminderState.Step.WAITING_FOR_TIME.name());
                reminderStateRepository.save(state);
                logger.info("Started time change flow for room: {}", roomId);
            }
        }
    }
}