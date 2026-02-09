package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 股票資訊 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockInfo {
    /**
     * 股票代號
     */
    private String symbol;

    /**
     * 股票名稱
     */
    private String name;

    /**
     * 當前價格
     */
    private BigDecimal currentPrice;

    /**
     * 開盤價
     */
    private BigDecimal openPrice;

    /**
     * 最高價
     */
    private BigDecimal highPrice;

    /**
     * 最低價
     */
    private BigDecimal lowPrice;

    /**
     * 昨收價
     */
    private BigDecimal previousClose;

    /**
     * 漲跌金額
     */
    private BigDecimal change;

    /**
     * 漲跌幅 (%)
     */
    private BigDecimal changePercent;

    /**
     * 成交量
     */
    private Long volume;

    /**
     * 更新時間
     */
    private LocalDateTime updateTime;

    /**
     * 漲停價
     */
    private BigDecimal limitUp;

    /**
     * 跌停價
     */
    private BigDecimal limitDown;

    /**
     * 最佳買價
     */
    private BigDecimal bestBidPrice;

    /**
     * 最佳賣價
     */
    private BigDecimal bestAskPrice;

    /**
     * 成交金額（百萬）
     */
    private BigDecimal turnover;
}