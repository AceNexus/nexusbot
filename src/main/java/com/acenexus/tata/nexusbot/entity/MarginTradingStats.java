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
 * 融資融券每日統計實體
 */
@Entity
@Table(name = "margin_trading_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_margin_stock_date", columnNames = {"stock_symbol", "trade_date"}),
        indexes = {
                @Index(name = "idx_margin_trade_date", columnList = "trade_date"),
                @Index(name = "idx_margin_stock_symbol", columnList = "stock_symbol")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginTradingStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    // --- 融資 (Margin Purchase) ---

    @Column(name = "margin_purchase_buy")
    private Long marginPurchaseBuy;

    @Column(name = "margin_purchase_sell")
    private Long marginPurchaseSell;

    @Column(name = "margin_purchase_cash_repayment")
    private Long marginPurchaseCashRepayment;

    @Column(name = "margin_purchase_yesterday_balance")
    private Long marginPurchaseYesterdayBalance;

    @Column(name = "margin_purchase_today_balance")
    private Long marginPurchaseTodayBalance;

    @Column(name = "margin_purchase_limit")
    private Long marginPurchaseLimit;

    @Column(name = "margin_purchase_change")
    private Long marginPurchaseChange;

    // --- 融券 (Short Sale) ---

    @Column(name = "short_sale_buy")
    private Long shortSaleBuy;

    @Column(name = "short_sale_sell")
    private Long shortSaleSell;

    @Column(name = "short_sale_cash_repayment")
    private Long shortSaleCashRepayment;

    @Column(name = "short_sale_yesterday_balance")
    private Long shortSaleYesterdayBalance;

    @Column(name = "short_sale_today_balance")
    private Long shortSaleTodayBalance;

    @Column(name = "short_sale_limit")
    private Long shortSaleLimit;

    @Column(name = "short_sale_change")
    private Long shortSaleChange;

    // --- 其他 ---

    @Column(name = "offset_loan_and_short")
    private Long offsetLoanAndShort;

    @Column(name = "utilization_rate", precision = 8, scale = 4)
    private BigDecimal utilizationRate;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
