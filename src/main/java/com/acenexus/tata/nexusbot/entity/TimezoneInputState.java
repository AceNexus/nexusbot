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
 * 時區輸入狀態實體
 * 用於追蹤正在設定時區的聊天室
 * 設計目的：
 * - 支援多實例部署環境下的狀態同步
 * - 取代內存 ConcurrentHashMap，避免狀態不一致
 * - 提供自動過期機制（30分鐘）
 */
@Entity
@Table(name = "timezone_input_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimezoneInputState {

    /**
     * 聊天室 ID（主鍵）
     */
    @Id
    @Column(name = "room_id", length = 100)
    private String roomId;

    /**
     * 解析後的時區 ID（IANA 格式）
     */
    @Column(name = "resolved_timezone", length = 50)
    private String resolvedTimezone;

    /**
     * 使用者原始輸入
     */
    @Column(name = "original_input", length = 100)
    private String originalInput;

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
