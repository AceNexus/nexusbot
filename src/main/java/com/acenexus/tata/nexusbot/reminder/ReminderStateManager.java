package com.acenexus.tata.nexusbot.reminder;

import com.acenexus.tata.nexusbot.entity.ReminderState;
import com.acenexus.tata.nexusbot.repository.ReminderStateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
     * 獲取當前步驟
     */
    public ReminderState.Step getCurrentStep(String roomId) {
        cleanupExpiredStates();
        return reminderStateRepository.findById(roomId)
                .map(state -> ReminderState.Step.valueOf(state.getStep()))
                .orElse(null);
    }

    /**
     * 獲取暫存的時間
     */
    public LocalDateTime getTime(String roomId) {
        return reminderStateRepository.findById(roomId)
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
     * 設置重複類型並進入下一步
     */
    @Transactional
    public void setRepeatType(String roomId, String repeatType) {
        Optional<ReminderState> optionalState = reminderStateRepository.findById(roomId);
        if (optionalState.isPresent()) {
            ReminderState state = optionalState.get();
            if (ReminderState.Step.WAITING_FOR_REPEAT_TYPE.name().equals(state.getStep())) {
                state.setRepeatType(repeatType);
                state.setStep(ReminderState.Step.WAITING_FOR_TIME.name());
                reminderStateRepository.save(state);
                logger.info("Set repeat type for room {}: {}", roomId, repeatType);
            }
        }
    }

    /**
     * 獲取重複類型
     */
    public String getRepeatType(String roomId) {
        return reminderStateRepository.findById(roomId)
                .map(ReminderState::getRepeatType)
                .orElse("ONCE");
    }

    /**
     * 清理過期的狀態記錄
     */
    @Transactional
    public void cleanupExpiredStates() {
        reminderStateRepository.deleteExpiredStates(LocalDateTime.now());
    }
}