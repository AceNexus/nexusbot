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
 * 聊天訊息實體
 * 記錄用戶與 AI 的完整對話流程，支援多輪對話追蹤和成本統計
 */
@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 聊天室 ID (關聯到 ChatRoom.roomId)
     * 個人聊天：userId
     * 群組聊天：groupId
     */
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    /**
     * 聊天室類型 (冗余設計提升查詢效能)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private ChatRoom.RoomType roomType;

    /**
     * 發送者 ID (LINE userId)
     * AI 訊息此欄位為 null
     */
    @Column(name = "user_id", length = 100)
    private String userId;

    /**
     * 訊息類型
     * USER: 用戶訊息
     * AI: AI 回應訊息
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    /**
     * 訊息內容
     * 支援長文本存儲
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * AI 處理使用的 tokens 數量
     * 用於統計 API 使用成本
     */
    @Column(name = "tokens_used")
    @Builder.Default
    private Integer tokensUsed = 0;

    /**
     * AI 處理時間 (毫秒)
     * 用於效能監控
     */
    @Column(name = "processing_time_ms")
    @Builder.Default
    private Integer processingTimeMs = 0;

    /**
     * 使用的 AI 模型名稱
     * 例如：llama-3.1-8b-instant
     */
    @Column(name = "ai_model", length = 50)
    private String aiModel;

    /**
     * 訊息建立時間
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 訊息更新時間
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 軟刪除時間
     * null 表示未刪除，有值表示已刪除
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 訊息類型列舉
     */
    public enum MessageType {
        USER,   // 用戶訊息
        AI      // AI 回應訊息
    }

    /**
     * 建立用戶訊息記錄
     */
    public static ChatMessage createUserMessage(String roomId, ChatRoom.RoomType roomType,
                                                String userId, String content) {
        return ChatMessage.builder()
                .roomId(roomId)
                .roomType(roomType)
                .userId(userId)
                .messageType(MessageType.USER)
                .content(content)
                .build();
    }

    /**
     * 建立 AI 回應訊息記錄
     */
    public static ChatMessage createAIMessage(String roomId, ChatRoom.RoomType roomType,
                                              String content, String aiModel,
                                              Integer tokensUsed, Integer processingTimeMs) {
        return ChatMessage.builder()
                .roomId(roomId)
                .roomType(roomType)
                .messageType(MessageType.AI)
                .content(content)
                .aiModel(aiModel)
                .tokensUsed(tokensUsed)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * 實體持久化前自動設定時間戳記
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 實體更新前自動設定更新時間戳記
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}