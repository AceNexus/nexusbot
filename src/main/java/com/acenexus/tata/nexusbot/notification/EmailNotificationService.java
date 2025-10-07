package com.acenexus.tata.nexusbot.notification;

import com.acenexus.tata.nexusbot.config.properties.EmailProperties;
import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Email 提醒通知服務
 * 負責發送提醒 Email 及生成確認連結
 */
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final ReminderLogRepository reminderLogRepository;
    private final TemplateEngine templateEngine;

    /**
     * 發送提醒 Email
     *
     * @param reminder       提醒資料
     * @param recipientEmail 收件者 Email
     * @return 是否發送成功
     */
    public boolean sendReminderEmail(Reminder reminder, String recipientEmail) {
        try {
            // 生成唯一確認 Token
            String confirmationToken = UUID.randomUUID().toString();
            String confirmationUrl = emailProperties.getConfirmationBaseUrl() + "/reminder/confirm/" + confirmationToken;

            // 創建 Email 內容
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
            helper.setTo(recipientEmail);
            helper.setSubject("📅 NexusBot 提醒通知");

            // 使用 Thymeleaf 模板生成 HTML 內容
            Context context = new Context();
            context.setVariable("reminderContent", reminder.getContent());
            context.setVariable("reminderTime", reminder.getReminderTime().format(TIME_FORMATTER));
            context.setVariable("confirmationUrl", confirmationUrl);
            context.setVariable("repeatType", getRepeatTypeText(reminder.getRepeatType()));

            String htmlContent = templateEngine.process("reminder-email", context);
            helper.setText(htmlContent, true);

            // 發送郵件
            mailSender.send(message);

            // 記錄發送日誌
            ReminderLog log = new ReminderLog();
            log.setReminderId(reminder.getId());
            log.setRoomId(reminder.getRoomId());
            log.setStatus("SENT");
            log.setDeliveryMethod("EMAIL");
            log.setConfirmationToken(confirmationToken);
            log.setSentTime(LocalDateTime.now());
            reminderLogRepository.save(log);

            logger.info("Reminder email sent successfully to {} for reminder {}", recipientEmail, reminder.getId());
            return true;

        } catch (MessagingException e) {
            logger.error("Failed to send reminder email to {} for reminder {}: {}", recipientEmail, reminder.getId(), e.getMessage());

            // 記錄失敗日誌
            ReminderLog log = new ReminderLog();
            log.setReminderId(reminder.getId());
            log.setRoomId(reminder.getRoomId());
            log.setStatus("FAILED");
            log.setDeliveryMethod("EMAIL");
            log.setErrorMessage("Email sending failed: " + e.getMessage());
            log.setSentTime(LocalDateTime.now());
            reminderLogRepository.save(log);

            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending reminder email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 轉換重複類型為可讀文字
     */
    private String getRepeatTypeText(String repeatType) {
        return switch (repeatType) {
            case "DAILY" -> "每日重複";
            case "WEEKLY" -> "每週重複";
            default -> "僅一次";
        };
    }
}
