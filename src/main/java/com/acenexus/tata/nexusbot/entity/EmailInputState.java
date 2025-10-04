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

/**
 * Email 輸入狀態實體
 * 用於追蹤正在等待輸入 Email 的聊天室
 * 設計目的：
 * - 支援多實例部署環境下的狀態同步
 * - 取代內存 ConcurrentHashMap，避免狀態不一致
 * - 提供自動過期機制（30分鐘）
 */
@Entity
@Table(name = "email_input_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailInputState {

    /**
     * 聊天室 ID（主鍵）
     */
    @Id
    @Column(name = "room_id", length = 100)
    private String roomId;

    /**
     * 建立時間
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 過期時間
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
