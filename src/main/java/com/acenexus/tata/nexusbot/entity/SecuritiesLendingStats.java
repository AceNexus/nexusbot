package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借券統計實體（每日每類型彙總）
 */
@Entity
@Table(name = "securities_lending_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_lending_stock_date_type", columnNames = {"stock_symbol", "trade_date", "transaction_type"}),
        indexes = {
                @Index(name = "idx_lending_trade_date", columnList = "trade_date"),
                @Index(name = "idx_lending_stock_symbol", columnList = "stock_symbol")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritiesLendingStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "transaction_type", length = 20)
    private String transactionType;

    @Column(name = "total_volume")
    private Long totalVolume;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "avg_fee_rate", precision = 8, scale = 4)
    private BigDecimal avgFeeRate;

    @Column(name = "close_price", precision = 12, scale = 2)
    private BigDecimal closePrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
