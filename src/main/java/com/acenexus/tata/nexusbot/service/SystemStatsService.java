package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import com.acenexus.tata.nexusbot.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemStatsService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 獲取系統統計資訊
     */
    public String getSystemStats() {
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
            long todayActiveRooms = chatMessageRepository.countTodayActiveRooms();
            long weekActiveRooms = chatMessageRepository.countThisWeekActiveRooms();

            // AI 性能統計
            Double avgProcessingTime = chatMessageRepository.getAverageProcessingTime();
            String avgTimeStr = avgProcessingTime != null ? String.format("%.1f ms", avgProcessingTime) : "無數據";

            return String.format("""
                            系統統計報告
                                            
                            聊天室概況
                            • 總聊天室：%d 個
                            • AI 啟用：%d 個 (%.1f%%)
                            • 管理員室：%d 個
                                            
                            消息統計
                            • 總消息數：%d 條
                            • 用戶消息：%d 條
                            • AI 回應：%d 條
                                            
                            活躍度
                            • 今日活躍：%d 個聊天室
                            • 本週活躍：%d 個聊天室
                                            
                            AI 性能
                            • 平均響應時間：%s
                                            
                            統計時間：%s
                            """,
                    totalRooms,
                    aiEnabledRooms,
                    totalRooms > 0 ? (aiEnabledRooms * 100.0 / totalRooms) : 0.0,
                    adminRooms,
                    totalMessages,
                    userMessages,
                    aiMessages,
                    todayActiveRooms,
                    weekActiveRooms,
                    avgTimeStr,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

        } catch (Exception e) {
            log.error("Failed to generate system stats", e);
            return "統計資料生成失敗：" + e.getMessage();
        }
    }
}