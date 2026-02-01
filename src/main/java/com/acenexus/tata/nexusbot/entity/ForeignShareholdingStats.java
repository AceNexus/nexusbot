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
 * 外資持股比例統計實體
 */
@Entity
@Table(name = "foreign_shareholding_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_foreign_stock_date", columnNames = {"stock_symbol", "trade_date"}),
        indexes = {
                @Index(name = "idx_foreign_trade_date", columnList = "trade_date"),
                @Index(name = "idx_foreign_stock_symbol", columnList = "stock_symbol")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignShareholdingStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "foreign_investment_shares")
    private Long foreignInvestmentShares;

    @Column(name = "foreign_investment_remaining_shares")
    private Long foreignInvestmentRemainingShares;

    @Column(name = "foreign_investment_shares_ratio", precision = 8, scale = 4)
    private BigDecimal foreignInvestmentSharesRatio;

    @Column(name = "foreign_investment_remain_ratio", precision = 8, scale = 4)
    private BigDecimal foreignInvestmentRemainRatio;

    @Column(name = "foreign_investment_upper_limit_ratio", precision = 8, scale = 4)
    private BigDecimal foreignInvestmentUpperLimitRatio;

    @Column(name = "number_of_shares_issued")
    private Long numberOfSharesIssued;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
