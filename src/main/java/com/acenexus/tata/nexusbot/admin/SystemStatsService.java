package com.acenexus.tata.nexusbot.admin;

import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import com.acenexus.tata.nexusbot.repository.ChatRoomRepository;
import com.acenexus.tata.nexusbot.repository.EmailRepository;
import com.acenexus.tata.nexusbot.repository.ReminderRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemStatsService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReminderRepository reminderRepository;
    private final EmailRepository emailRepository;
    private final MessageTemplateProvider messageTemplateProvider;

    /**
     * 獲取系統統計資訊 Message
     */
    public Message getSystemStats() {
        try {
            // 聊天室統計
            long totalRooms = chatRoomRepository.countTotalRooms();
            long aiEnabledRooms = chatRoomRepository.countAiEnabledRooms();
            long adminRooms = chatRoomRepository.countAdminRooms();

            // 消息統計
            long totalMessages = chatMessageRepository.countTotalMessages();
            long aiMessages = chatMessageRepository.countAiMessages();
            long userMessages = chatMessageRepository.countUserMessages();

            // 活躍度統計
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            long todayActiveRooms = chatMessageRepository.countActiveRoomsSince(todayStart);
            long weekActiveRooms = chatMessageRepository.countActiveRoomsSince(weekStart);

            // AI 性能與成本統計
            Double avgProcessingTime = chatMessageRepository.getAverageProcessingTime();
            String avgTimeStr = avgProcessingTime != null ? String.format("%.1f ms", avgProcessingTime) : "無數據";
            long totalTokensUsed = chatMessageRepository.sumTotalTokensUsed();

            // 提醒統計
            long activeReminders = reminderRepository.countActiveReminders();

            // Email 統計
            long activeEmails = emailRepository.countActiveEmails();

            return messageTemplateProvider.systemStats(
                    totalRooms, aiEnabledRooms, adminRooms,
                    totalMessages, userMessages, aiMessages,
                    todayActiveRooms, weekActiveRooms,
                    avgTimeStr, totalTokensUsed,
                    activeReminders, activeEmails
            );

        } catch (Exception e) {
            log.error("Failed to generate system stats", e);
            return messageTemplateProvider.error("統計資料生成失敗：" + e.getMessage());
        }
    }
}