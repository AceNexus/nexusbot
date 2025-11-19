package com.acenexus.tata.nexusbot.email;

import com.acenexus.tata.nexusbot.entity.EmailInputState;
import com.acenexus.tata.nexusbot.repository.EmailInputStateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Email 輸入狀態服務
 * 管理用戶新增 Email 時的輸入狀態
 */
@Service
@RequiredArgsConstructor
public class EmailInputStateService {

    private static final Logger logger = LoggerFactory.getLogger(EmailInputStateService.class);
    private static final int STATE_EXPIRY_MINUTES = 30;

    private final EmailInputStateRepository emailInputStateRepository;

    /**
     * 檢查聊天室是否正在等待 Email 輸入
     *
     * @param roomId 聊天室 ID
     * @return 是否正在等待輸入
     */
    public boolean isWaitingForEmailInput(String roomId) {
        return emailInputStateRepository.existsByRoomIdAndNotExpired(roomId, LocalDateTime.now());
    }

    /**
     * 設置聊天室為等待 Email 輸入狀態
     *
     * @param roomId 聊天室 ID
     */
    public void setWaitingForEmailInput(String roomId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(STATE_EXPIRY_MINUTES);

        EmailInputState state = EmailInputState.builder()
                .roomId(roomId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        emailInputStateRepository.save(state);
        logger.info("Set email input state for room {}, expires at {}", roomId, expiresAt);
    }

    /**
     * 清除聊天室的 Email 輸入等待狀態
     *
     * @param roomId 聊天室 ID
     */
    public void clearWaitingForEmailInput(String roomId) {
        emailInputStateRepository.deleteById(roomId);
        logger.info("Cleared email input state for room {}", roomId);
    }
}
