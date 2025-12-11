package com.acenexus.tata.nexusbot.timezone;

import com.acenexus.tata.nexusbot.entity.TimezoneInputState;
import com.acenexus.tata.nexusbot.repository.TimezoneInputStateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 時區輸入狀態服務
 * 管理用戶變更時區時的輸入狀態
 */
@Service
@RequiredArgsConstructor
public class TimezoneInputStateService {

    private static final Logger logger = LoggerFactory.getLogger(TimezoneInputStateService.class);
    private static final int STATE_EXPIRY_MINUTES = 30;

    private final TimezoneInputStateRepository timezoneInputStateRepository;

    /**
     * 檢查聊天室是否正在等待時區輸入
     *
     * @param roomId 聊天室 ID
     * @return 是否正在等待輸入
     */
    public boolean isWaitingForTimezoneInput(String roomId) {
        return timezoneInputStateRepository.existsByRoomIdAndNotExpired(roomId, LocalDateTime.now());
    }

    /**
     * 設置聊天室為等待時區輸入狀態
     *
     * @param roomId 聊天室 ID
     */
    public void setWaitingForTimezoneInput(String roomId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(STATE_EXPIRY_MINUTES);

        TimezoneInputState state = TimezoneInputState.builder()
                .roomId(roomId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        timezoneInputStateRepository.save(state);
        logger.info("Set timezone input state for room {}, expires at {}", roomId, expiresAt);
    }

    /**
     * 儲存解析後的時區到輸入狀態
     *
     * @param roomId           聊天室 ID
     * @param resolvedTimezone 解析後的時區 ID
     * @param originalInput    使用者原始輸入
     */
    public void saveResolvedTimezone(String roomId, String resolvedTimezone, String originalInput) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(STATE_EXPIRY_MINUTES);

        TimezoneInputState state = TimezoneInputState.builder()
                .roomId(roomId)
                .resolvedTimezone(resolvedTimezone)
                .originalInput(originalInput)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        timezoneInputStateRepository.save(state);
        logger.info("Saved resolved timezone for room {}: {} (from input: {})", roomId, resolvedTimezone, originalInput);
    }

    /**
     * 獲取聊天室的時區輸入狀態
     *
     * @param roomId 聊天室 ID
     * @return 時區輸入狀態（可能為空）
     */
    public Optional<TimezoneInputState> getTimezoneInputState(String roomId) {
        return timezoneInputStateRepository.findByRoomIdAndNotExpired(roomId, LocalDateTime.now());
    }

    /**
     * 清除聊天室的時區輸入等待狀態
     *
     * @param roomId 聊天室 ID
     */
    public void clearWaitingForTimezoneInput(String roomId) {
        timezoneInputStateRepository.deleteById(roomId);
        logger.info("Cleared timezone input state for room {}", roomId);
    }
}
