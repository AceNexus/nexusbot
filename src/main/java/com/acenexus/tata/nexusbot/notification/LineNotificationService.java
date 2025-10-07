package com.acenexus.tata.nexusbot.notification;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * LINE 通知服務
 * 負責發送 LINE Push Message 提醒通知
 */
@Service
@RequiredArgsConstructor
public class LineNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(LineNotificationService.class);

    private final LineMessagingClient lineMessagingClient;
    private final MessageTemplateProvider messageTemplateProvider;
    private final ReminderLogRepository reminderLogRepository;

    /**
     * 發送 LINE 提醒通知
     *
     * @param reminder        提醒資料
     * @param enhancedContent AI 增強後的提醒內容
     * @return 是否發送成功
     */
    public boolean pushReminder(Reminder reminder, String enhancedContent) {
        try {
            Message reminderMessage = messageTemplateProvider.buildReminderNotification(enhancedContent, reminder.getContent(), reminder.getRepeatType(), reminder.getId());

            PushMessage pushMessage = new PushMessage(reminder.getRoomId(), reminderMessage);
            lineMessagingClient.pushMessage(pushMessage);

            // 記錄發送日誌
            saveReminderLog(reminder, "SENT", null);

            logger.info("LINE notification sent successfully for reminder [{}]", reminder.getId());
            return true;

        } catch (Exception e) {
            logger.error("Failed to send LINE notification for reminder [{}]: {}", reminder.getId(), e.getMessage());

            // 記錄失敗日誌
            saveReminderLog(reminder, "FAILED", e.getMessage());
            return false;
        }
    }

    /**
     * 儲存提醒發送日誌
     */
    private void saveReminderLog(Reminder reminder, String status, String errorMessage) {
        try {
            ReminderLog log = new ReminderLog();
            log.setReminderId(reminder.getId());
            log.setRoomId(reminder.getRoomId());
            log.setStatus(status);
            log.setDeliveryMethod("LINE");
            log.setSentTime(LocalDateTime.now());

            if (errorMessage != null) {
                log.setErrorMessage(errorMessage);
            }

            reminderLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to save reminder log: {}", e.getMessage());
        }
    }
}
