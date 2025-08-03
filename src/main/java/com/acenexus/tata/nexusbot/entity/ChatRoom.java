package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天室實體
 * 記錄每個聊天室的 AI 回應設定狀態
 */
@Entity
@Table(name = "chat_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 聊天室 ID (LINE 的 userId 或 groupId)
     * 個人聊天：userId
     * 群組聊天：groupId
     */
    @Column(name = "room_id", nullable = false, unique = true, length = 100)
    private String roomId;

    /**
     * 聊天室類型
     * USER: 個人聊天
     * GROUP: 群組聊天
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    /**
     * AI 回應功能是否啟用
     * 預設：false (關閉)
     */
    @Column(name = "ai_enabled", nullable = false)
    @Builder.Default
    private Boolean aiEnabled = false;

    /**
     * 建立時間
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 最後更新時間
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 聊天室類型列舉
     */
    public enum RoomType {
        USER,   // 個人聊天
        GROUP   // 群組聊天
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}