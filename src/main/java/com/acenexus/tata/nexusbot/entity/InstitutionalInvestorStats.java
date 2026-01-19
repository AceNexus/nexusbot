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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 法人每日買賣超統計實體
 */
@Entity
@Table(name = "institutional_investor_stats", 
       uniqueConstraints = @UniqueConstraint(name = "uk_stock_date", columnNames = {"stock_symbol", "trade_date"}),
       indexes = {
           @Index(name = "idx_trade_date", columnList = "trade_date"),
           @Index(name = "idx_stock_symbol", columnList = "stock_symbol")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionalInvestorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    // 外資
    @Column(name = "foreign_investor_buy")
    private Long foreignInvestorBuy;

    @Column(name = "foreign_investor_sell")
    private Long foreignInvestorSell;

    @Column(name = "foreign_investor_buy_sell")
    private Long foreignInvestorBuySell;

    // 投信
    @Column(name = "investment_trust_buy")
    private Long investmentTrustBuy;

    @Column(name = "investment_trust_sell")
    private Long investmentTrustSell;

    @Column(name = "investment_trust_buy_sell")
    private Long investmentTrustBuySell;

    // 自營商
    @Column(name = "dealer_buy")
    private Long dealerBuy;

    @Column(name = "dealer_sell")
    private Long dealerSell;

    @Column(name = "dealer_buy_sell")
    private Long dealerBuySell;

    // 合計
    @Column(name = "total_buy")
    private Long totalBuy;

    @Column(name = "total_sell")
    private Long totalSell;

    @Column(name = "total_buy_sell")
    private Long totalBuySell;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
