package com.acenexus.tata.nexusbot.notification;

import com.acenexus.tata.nexusbot.email.EmailManager;
import com.acenexus.tata.nexusbot.entity.Reminder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 提醒通知服務實作
 * 協調 LINE 和 Email 通知的發送
 * 設計特色：
 * - 統一通知路由邏輯
 * - 支援多種通知管道
 * - 易於擴充新通知管道（Push、SMS 等）
 */
@Service
@RequiredArgsConstructor
public class ReminderNotificationServiceImpl implements ReminderNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderNotificationServiceImpl.class);

    private final LineNotificationService lineNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final EmailManager emailManager;

    @Override
    public void send(Reminder reminder, String enhancedContent) {
        String channel = reminder.getNotificationChannel() != null ? reminder.getNotificationChannel() : "LINE";
        logger.info("Sending reminder [{}] via channel: {}", reminder.getId(), channel);

        switch (channel.toUpperCase()) {
            case "LINE" -> sendLineOnly(reminder, enhancedContent);
            case "EMAIL" -> sendEmailOnly(reminder);
            case "BOTH" -> sendBoth(reminder, enhancedContent);
            default -> {
                logger.warn("Unknown notification channel '{}' for reminder [{}], using LINE", channel, reminder.getId());
                sendLineOnly(reminder, enhancedContent);
            }
        }
    }

    @Override
    public void sendLineOnly(Reminder reminder, String enhancedContent) {
        try {
            boolean success = lineNotificationService.pushReminder(reminder, enhancedContent);
            if (success) {
                logger.info("LINE notification sent for reminder [{}]", reminder.getId());
            } else {
                logger.error("LINE notification failed for reminder [{}]", reminder.getId());
            }
        } catch (Exception e) {
            logger.error("Exception sending LINE notification for reminder [{}]: {}", reminder.getId(), e.getMessage());
        }
    }

    @Override
    public void sendEmailOnly(Reminder reminder) {
        try {
            List<String> enabledEmails = emailManager.getEnabledEmailAddresses(reminder.getRoomId());

            if (enabledEmails.isEmpty()) {
                logger.warn("No enabled email addresses for room [{}], skipping email notification", reminder.getRoomId());
                return;
            }

            int successCount = 0;
            for (String email : enabledEmails) {
                try {
                    boolean sent = emailNotificationService.sendReminderEmail(reminder, email);
                    if (sent) {
                        successCount++;
                        logger.debug("Email sent successfully to {} for reminder [{}]", email, reminder.getId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to send email to {} for reminder [{}]: {}", email, reminder.getId(), e.getMessage());
                }
            }

            logger.info("Email notification completed for reminder [{}]: {}/{} sent", reminder.getId(), successCount, enabledEmails.size());

        } catch (Exception e) {
            logger.error("Exception sending email notification for reminder [{}]: {}", reminder.getId(), e.getMessage());
        }
    }

    @Override
    public void sendBoth(Reminder reminder, String enhancedContent) {
        logger.info("Sending reminder [{}] via both LINE and Email", reminder.getId());

        // 並行發送 LINE 和 Email
        try {
            sendLineOnly(reminder, enhancedContent);
        } catch (Exception e) {
            logger.error("LINE notification failed in BOTH mode: {}", e.getMessage());
        }

        try {
            sendEmailOnly(reminder);
        } catch (Exception e) {
            logger.error("Email notification failed in BOTH mode: {}", e.getMessage());
        }

        logger.info("Both notifications completed for reminder [{}]", reminder.getId());
    }
}
