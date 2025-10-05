package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderState {

    @Id
    @Column(name = "room_id", length = 100)
    private String roomId;

    /**
     * 當前步驟
     */
    @Column(name = "step", nullable = false, length = 50)
    private String step;

    /**
     * 重複類型：ONCE, DAILY, WEEKLY
     */
    @Column(name = "repeat_type", length = 20)
    private String repeatType;

    /**
     * 暫存的提醒時間
     */
    @Column(name = "reminder_time")
    private LocalDateTime reminderTime;

    /**
     * 建立時間
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 過期時間（30分鐘後）
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 通知管道：LINE, EMAIL, BOTH
     */
    @Column(name = "notification_channel", length = 20)
    @Builder.Default
    private String notificationChannel = "LINE";

    /**
     * 步驟枚舉
     */
    public enum Step {
        WAITING_FOR_REPEAT_TYPE,
        WAITING_FOR_NOTIFICATION_CHANNEL,
        WAITING_FOR_TIME,
        WAITING_FOR_CONTENT
    }
}