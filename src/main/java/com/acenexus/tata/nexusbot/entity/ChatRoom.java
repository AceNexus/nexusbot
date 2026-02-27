package com.acenexus.tata.nexusbot.entity;

import com.acenexus.tata.nexusbot.config.properties.TimezoneProperties;
import com.acenexus.tata.nexusbot.constants.AiModel;
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
     * 選擇的AI模型，預設為 {@link com.acenexus.tata.nexusbot.constants.AiModel#LLAMA_3_1_8B}
     */
    @Column(name = "ai_model", length = 50)
    @Builder.Default
    private String aiModel = AiModel.LLAMA_3_1_8B.id;

    /**
     * 是否為管理員聊天室
     * 預設：false (非管理員)
     */
    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    /**
     * 是否正在等待密碼輸入
     * 預設：false (非等待狀態)
     */
    @Column(name = "auth_pending", nullable = false)
    @Builder.Default
    private Boolean authPending = false;

    /**
     * 是否正在等待位置以搜尋廁所
     * 預設：false (非等待狀態)
     */
    @Column(name = "waiting_for_location", nullable = false)
    @Builder.Default
    private Boolean waitingForLocation = false;

    /**
     * 使用者當前所在時區
     * 預設：Asia/Taipei（台灣時區）
     * 用於解析新提醒的時間，可隨時修改
     */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = TimezoneProperties.FALLBACK_DEFAULT;

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