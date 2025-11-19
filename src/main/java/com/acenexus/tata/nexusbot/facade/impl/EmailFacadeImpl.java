package com.acenexus.tata.nexusbot.facade.impl;

import com.acenexus.tata.nexusbot.email.EmailInputStateService;
import com.acenexus.tata.nexusbot.email.EmailManager;
import com.acenexus.tata.nexusbot.entity.Email;
import com.acenexus.tata.nexusbot.facade.EmailFacade;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Email 功能 Facade 實作
 */
@Service
@RequiredArgsConstructor
public class EmailFacadeImpl implements EmailFacade {

    private static final Logger logger = LoggerFactory.getLogger(EmailFacadeImpl.class);
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final EmailManager emailManager;
    private final EmailInputStateService emailInputStateService;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public Message showMenu(String roomId) {
        List<Email> emails = emailManager.getActiveEmails(roomId);
        logger.debug("Retrieved {} emails for room: {}", emails.size(), roomId);
        return messageTemplateProvider.emailSettingsMenu(emails);
    }

    @Override
    public Message startAddingEmail(String roomId) {
        emailInputStateService.setWaitingForEmailInput(roomId);
        logger.info("Room {} is now waiting for email input", roomId);
        return messageTemplateProvider.emailInputPrompt();
    }

    @Override
    public Message cancelAddingEmail(String roomId) {
        emailInputStateService.clearWaitingForEmailInput(roomId);
        logger.info("Cancelled email input for room {}", roomId);
        return messageTemplateProvider.success("已取消新增 Email");
    }

    @Override
    public Message deleteEmail(Long emailId, String roomId) {
        try {
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

    @Override
    public Message toggleEmailStatus(Long emailId, String roomId) {
        try {
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

    @Override
    public Message handleEmailInput(String roomId, String email) {
        try {
            email = email.trim();

            // 驗證 Email 格式
            if (!email.matches(EMAIL_REGEX)) {
                logger.warn("Invalid email format from room {}: {}", roomId, email);
                return messageTemplateProvider.emailInvalidFormat();
            }

            // 新增 Email
            Email addedEmail = emailManager.addEmail(roomId, email);

            if (addedEmail != null) {
                // 清除等待狀態
                emailInputStateService.clearWaitingForEmailInput(roomId);

                logger.info("Email added successfully for room {}: {}", roomId, email);
                return messageTemplateProvider.emailAddSuccess(email);
            } else {
                logger.error("Failed to add email for room {}", roomId);
                return messageTemplateProvider.error("新增 Email 時發生錯誤,請稍後再試。");
            }
        } catch (Exception e) {
            logger.error("Error processing email input for room {}: {}", roomId, e.getMessage());
            emailInputStateService.clearWaitingForEmailInput(roomId);
            return messageTemplateProvider.error("處理 Email 輸入時發生錯誤。");
        }
    }

    @Override
    public boolean isWaitingForEmailInput(String roomId) {
        return emailInputStateService.isWaitingForEmailInput(roomId);
    }

    @Override
    public void clearEmailInputState(String roomId) {
        emailInputStateService.clearWaitingForEmailInput(roomId);
    }
}
