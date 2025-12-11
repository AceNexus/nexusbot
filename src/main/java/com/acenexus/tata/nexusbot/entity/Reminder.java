package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 聊天室ID
     */
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    /**
     * 提醒內容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 提醒時區（建立時固化）
     * 代表這個提醒是在哪個時區建立的
     * 例如：Asia/Taipei, Asia/Tokyo, America/New_York
     */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Taipei";

    /**
     * 提醒時間的 UTC 時間戳（毫秒）
     * 用於排程器精確判斷發送時間
     * 儲存為 BIGINT (epoch milliseconds)
     * 注意：舊資料可能為 NULL，需由遷移程式處理
     */
    @Column(name = "reminder_time_instant")
    private Long reminderTimeInstant;

    /**
     * 重複類型：ONCE, DAILY, WEEKLY
     */
    @Column(name = "repeat_type", length = 20)
    @Builder.Default
    private String repeatType = "ONCE";

    /**
     * 狀態：ACTIVE, PAUSED, COMPLETED
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * 建立者ID
     */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    /**
     * 通知管道：LINE, EMAIL, BOTH
     */
    @Column(name = "notification_channel", length = 20)
    @Builder.Default
    private String notificationChannel = "LINE";

    /**
     * 建立時間
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 從 instant + timezone 計算正確的本地時間
     * 這是顯示用的唯一正確方法，避免時區混淆
     *
     * @return 該時區的本地時間
     */
    public LocalDateTime getLocalTime() {
        if (reminderTimeInstant == null) {
            throw new IllegalStateException("reminderTimeInstant cannot be null");
        }
        Instant instant = Instant.ofEpochMilli(reminderTimeInstant);
        String tz = timezone != null ? timezone : "Asia/Taipei";
        return instant.atZone(ZoneId.of(tz)).toLocalDateTime();
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", roomId='" + roomId + '\'' +
                ", content='" + content + '\'' +
                ", reminderTime=" + (reminderTimeInstant != null
                ? Instant.ofEpochMilli(reminderTimeInstant).atZone(ZoneId.of(timezone)).toLocalDateTime()
                : null) +
                ", repeatType='" + repeatType + '\'' +
                ", status='" + status + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", notificationChannel='" + notificationChannel + '\'' +
                ", timezone='" + timezone + '\'' +
                '}';
    }
}