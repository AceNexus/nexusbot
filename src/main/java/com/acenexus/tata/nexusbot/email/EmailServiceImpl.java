package com.acenexus.tata.nexusbot.email;

import com.acenexus.tata.nexusbot.config.properties.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * 電子郵件服務實作
 * 使用 JavaMailSender 發送郵件
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Override
    public boolean sendEmail(String to, String subject, String content, boolean isHtml) {
        try {
            // 檢查收件者信箱是否有效
            if (to == null || to.trim().isEmpty()) {
                logger.warn("Recipient email address is empty, cannot send email");
                return false;
            }

            // 檢查必要的配置
            if (emailProperties.getFrom() == null || emailProperties.getFrom().trim().isEmpty()) {
                logger.error("Sender email address is not configured");
                return false;
            }

            // 建立郵件訊息
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 設定寄件者
            String fromName = emailProperties.getFromName() != null ? emailProperties.getFromName() : "NexusBot";
            helper.setFrom(emailProperties.getFrom(), fromName);

            // 設定收件者
            helper.setTo(to);

            // 設定主旨
            helper.setSubject(subject);

            // 設定內容
            helper.setText(content, isHtml);

            // 發送郵件
            mailSender.send(message);

            logger.info("Successfully sent email to: {}, subject: {}", to, subject);
            return true;

        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}, MessagingException: {}", to, e.getMessage(), e);
            return false;
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to encode sender name: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while sending email to: {}, Error: {}", to, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendReminderEmail(String to, String reminderTime, String enhancedContent, String originalContent) {
        String subject = "NexusBot 提醒通知";

        // 建立 HTML 郵件內容
        String htmlContent = buildReminderEmailContent(reminderTime, enhancedContent, originalContent);

        return sendEmail(to, subject, htmlContent, true);
    }

    /**
     * 建立提醒郵件的 HTML 內容
     */
    private String buildReminderEmailContent(String reminderTime, String enhancedContent, String originalContent) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="zh-TW">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>提醒通知</title>
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #00A8CC; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 24px;">提醒時間到了</h1>
                    </div>
                    <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; border: 1px solid #ddd; border-top: none;">
                        <div style="background-color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                            <h2 style="color: #00A8CC; margin-top: 0; font-size: 18px;">提醒時間</h2>
                            <p style="font-size: 16px; margin: 10px 0; color: #555;">%s</p>
                        </div>
                        <div style="background-color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                            <h2 style="color: #00A8CC; margin-top: 0; font-size: 18px;">提醒事項</h2>
                            <p style="font-size: 16px; margin: 10px 0; white-space: pre-wrap; color: #555;">%s</p>
                        </div>
                        <div style="background-color: white; padding: 20px; border-radius: 8px;">
                            <h2 style="color: #00A8CC; margin-top: 0; font-size: 18px;">貼心小提醒</h2>
                            <p style="font-size: 16px; margin: 10px 0; white-space: pre-wrap; color: #555;">%s</p>
                        </div>
                    </div>
                    <div style="margin-top: 20px; padding: 20px; text-align: center; color: #888; font-size: 14px;">
                        <p style="margin: 5px 0;">此郵件由 NexusBot 自動發送</p>
                        <p style="margin: 5px 0;">如需修改郵件通知設定，請在 LINE Bot 中操作</p>
                    </div>
                </body>
                </html>
                """, reminderTime, originalContent, enhancedContent);
    }
}
