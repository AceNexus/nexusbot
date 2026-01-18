package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 股票群組實體
 * 記錄使用者自訂的股票群組
 */
@Entity
@Table(name = "stock_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 使用者 ID (LINE userId 或匿名 UUID)
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /**
     * 群組名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 顯示順序
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * 是否啟用（軟刪除標記）
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 是否為目前選中的群組（用於狀態恢復）
     */
    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private Boolean isSelected = false;

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
