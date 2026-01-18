package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 股票群組項目實體
 * 記錄群組內的股票
 */
@Entity
@Table(name = "stock_group_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockGroupItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 群組 ID
     */
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /**
     * 股票代號
     */
    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    /**
     * 股票名稱
     */
    @Column(name = "stock_name", length = 100)
    private String stockName;

    /**
     * 市場類型 (上市/上櫃)
     */
    @Column(name = "market", length = 10)
    private String market;

    /**
     * 顯示順序
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * 建立時間
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
