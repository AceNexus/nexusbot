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

    /**
     * 主鍵 ID (自動遞增)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代號 (例如: 2330)
     */
    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    /**
     * 股票名稱 (例如: 台積電)
     */
    @Column(name = "stock_name", length = 100)
    private String stockName;

    /**
     * 交易日期
     */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    // --- 外資 (Foreign Investor) ---

    /**
     * 外資買進股數 (單位: 股)
     */
    @Column(name = "foreign_investor_buy")
    private Long foreignInvestorBuy;

    /**
     * 外資賣出股數 (單位: 股)
     */
    @Column(name = "foreign_investor_sell")
    private Long foreignInvestorSell;

    /**
     * 外資買賣超股數 (單位: 股, 正數為買超, 負數為賣超)
     */
    @Column(name = "foreign_investor_buy_sell")
    private Long foreignInvestorBuySell;

    // --- 投信 (Investment Trust) ---

    /**
     * 投信買進股數 (單位: 股)
     */
    @Column(name = "investment_trust_buy")
    private Long investmentTrustBuy;

    /**
     * 投信賣出股數 (單位: 股)
     */
    @Column(name = "investment_trust_sell")
    private Long investmentTrustSell;

    /**
     * 投信買賣超股數 (單位: 股, 正數為買超, 負數為賣超)
     */
    @Column(name = "investment_trust_buy_sell")
    private Long investmentTrustBuySell;

    // --- 自營商 (Dealer) ---

    /**
     * 自營商買進股數 (單位: 股)
     */
    @Column(name = "dealer_buy")
    private Long dealerBuy;

    /**
     * 自營商賣出股數 (單位: 股)
     */
    @Column(name = "dealer_sell")
    private Long dealerSell;

    /**
     * 自營商買賣超股數 (單位: 股, 正數為買超, 負數為賣超)
     */
    @Column(name = "dealer_buy_sell")
    private Long dealerBuySell;

    // --- 三大法人合計 (Total) ---

    /**
     * 三大法人買進股數合計 (單位: 股)
     */
    @Column(name = "total_buy")
    private Long totalBuy;

    /**
     * 三大法人賣出股數合計 (單位: 股)
     */
    @Column(name = "total_sell")
    private Long totalSell;

    /**
     * 三大法人買賣超股數合計 (單位: 股, 正數為買超, 負數為賣超)
     */
    @Column(name = "total_buy_sell")
    private Long totalBuySell;

    /**
     * 資料建立時間 (自動生成)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
