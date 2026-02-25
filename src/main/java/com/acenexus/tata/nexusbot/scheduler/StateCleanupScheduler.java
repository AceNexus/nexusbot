package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.repository.EmailInputStateRepository;
import com.acenexus.tata.nexusbot.repository.ReminderStateRepository;
import com.acenexus.tata.nexusbot.repository.TimezoneInputStateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 定期清理三張過期狀態表：
 * - reminder_states
 * - email_input_states
 * - timezone_input_states
 * <p>
 * 取代原本 ReminderStateManager.getState() 內嵌的 cleanupExpiredStates() 呼叫，
 * 確保 canHandle() 不再有 DB 寫入副作用。
 */
@Component
@RequiredArgsConstructor
public class StateCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(StateCleanupScheduler.class);

    private final ReminderStateRepository reminderStateRepository;
    private final EmailInputStateRepository emailInputStateRepository;
    private final TimezoneInputStateRepository timezoneInputStateRepository;

    @Scheduled(fixedRate = 600_000) // 每 10 分鐘執行一次
    @Transactional
    public void cleanupExpiredStates() {
        LocalDateTime now = LocalDateTime.now();
        try {
            int reminder = reminderStateRepository.deleteExpiredStates(now);
            int email = emailInputStateRepository.deleteExpiredStates(now);
            int timezone = timezoneInputStateRepository.deleteExpiredStates(now);
            if (reminder + email + timezone > 0) {
                logger.info("Cleaned up expired states — reminder: {}, email: {}, timezone: {}", reminder, email, timezone);
            }
        } catch (Exception e) {
            logger.error("Failed to clean up expired states: {}", e.getMessage(), e);
        }
    }
}
