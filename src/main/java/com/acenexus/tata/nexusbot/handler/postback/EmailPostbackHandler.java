package com.acenexus.tata.nexusbot.handler.postback;

import com.acenexus.tata.nexusbot.email.EmailManager;
import com.acenexus.tata.nexusbot.entity.Email;
import com.acenexus.tata.nexusbot.entity.EmailInputState;
import com.acenexus.tata.nexusbot.repository.EmailInputStateRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_EMAIL_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.DELETE_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.EMAIL_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_EMAIL_STATUS;

/**
 * Email 功能 Handler - 處理 Email 管理、新增、刪除、啟用/停用
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class EmailPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(EmailPostbackHandler.class);

    private final EmailManager emailManager;
    private final EmailInputStateRepository emailInputStateRepository;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(String action) {
        // 靜態動作
        if (EMAIL_MENU.equals(action) || ADD_EMAIL.equals(action) || CANCEL_EMAIL_INPUT.equals(action)) {
            return true;
        }

        // 動態動作（包含參數）
        return action.startsWith(DELETE_EMAIL) || action.startsWith(TOGGLE_EMAIL_STATUS);
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("EmailPostbackHandler handling action: {} for room: {}", action, roomId);

        // 處理動態動作
        if (action.startsWith(DELETE_EMAIL)) {
            return handleDeleteEmail(action, roomId);
        }

        if (action.startsWith(TOGGLE_EMAIL_STATUS)) {
            return handleToggleEmailStatus(action, roomId);
        }

        // 處理靜態動作
        return switch (action) {
            case EMAIL_MENU -> {
                List<Email> emails = emailManager.getActiveEmails(roomId);
                logger.debug("Retrieved {} emails for room: {}", emails.size(), roomId);
                yield messageTemplateProvider.emailSettingsMenu(emails);
            }

            case ADD_EMAIL -> {
                setWaitingForEmailInput(roomId);
                logger.info("Room {} is now waiting for email input", roomId);
                yield messageTemplateProvider.emailInputPrompt();
            }

            case CANCEL_EMAIL_INPUT -> {
                clearWaitingForEmailInput(roomId);
                logger.info("Cancelled email input for room {}", roomId);
                yield messageTemplateProvider.success("已取消新增 Email");
            }

            default -> {
                logger.warn("Unexpected action in EmailPostbackHandler: {}", action);
                yield null;
            }
        };
    }

    private Message handleDeleteEmail(String data, String roomId) {
        try {
            String idStr = data.substring(data.indexOf("&id=") + 4);
            Long emailId = Long.parseLong(idStr);

            boolean success = emailManager.deleteEmail(emailId, roomId);
            if (success) {
                logger.info("Deleted email {} for room: {}", emailId, roomId);
                return messageTemplateProvider.success("Email 已刪除");
            } else {
                logger.warn("Failed to delete email {} for room: {}", emailId, roomId);
                return messageTemplateProvider.error("刪除 Email 失敗");
            }

        } catch (Exception e) {
            logger.error("Delete email error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("刪除 Email 時發生錯誤");
        }
    }

    private Message handleToggleEmailStatus(String data, String roomId) {
        try {
            String idStr = data.substring(data.indexOf("&id=") + 4);
            Long emailId = Long.parseLong(idStr);

            List<Email> emails = emailManager.getActiveEmails(roomId);
            Email targetEmail = emails.stream()
                    .filter(e -> e.getId().equals(emailId))
                    .findFirst()
                    .orElse(null);

            if (targetEmail == null) {
                logger.warn("Email {} not found for room: {}", emailId, roomId);
                return messageTemplateProvider.error("Email 不存在");
            }

            boolean success;
            String message;
            if (Boolean.TRUE.equals(targetEmail.getIsEnabled())) {
                success = emailManager.disableEmail(emailId, roomId);
                message = success ? "Email 通知已停用" : "停用 Email 通知失敗";
                logger.info("Disabled email {} for room: {}", emailId, roomId);
            } else {
                success = emailManager.enableEmail(emailId, roomId);
                message = success ? "Email 通知已啟用" : "啟用 Email 通知失敗";
                logger.info("Enabled email {} for room: {}", emailId, roomId);
            }

            return success ? messageTemplateProvider.success(message) : messageTemplateProvider.error(message);

        } catch (Exception e) {
            logger.error("Toggle email status error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("切換 Email 狀態時發生錯誤");
        }
    }

    private void setWaitingForEmailInput(String roomId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(30);

        EmailInputState state = EmailInputState.builder()
                .roomId(roomId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        emailInputStateRepository.save(state);
        logger.info("Set email input state for room {}, expires at {}", roomId, expiresAt);
    }

    private void clearWaitingForEmailInput(String roomId) {
        emailInputStateRepository.deleteById(roomId);
        logger.info("Cleared email input state for room {}", roomId);
    }

    public boolean isWaitingForEmailInput(String roomId) {
        return emailInputStateRepository.existsByRoomIdAndNotExpired(roomId, LocalDateTime.now());
    }

    public void clearEmailInputState(String roomId) {
        clearWaitingForEmailInput(roomId);
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
