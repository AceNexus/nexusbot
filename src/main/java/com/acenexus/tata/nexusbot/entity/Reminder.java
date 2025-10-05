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

import java.time.LocalDateTime;

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
     * 提醒時間
     */
    @Column(name = "reminder_time", nullable = false)
    private LocalDateTime reminderTime;

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

}