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

/**
 * Email 實體
 * 記錄每個聊天室綁定的電子郵件地址
 * 一個聊天室可以有多個 Email
 */
@Entity
@Table(name = "emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 聊天室 ID (對應 chat_rooms.room_id)
     */
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    /**
     * 電子郵件地址
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * 是否啟用此信箱的 Email 通知
     * 預設：true (啟用)
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 是否為啟用狀態（軟刪除用）
     * 預設：true (啟用)
     * false 表示已被刪除
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

}
