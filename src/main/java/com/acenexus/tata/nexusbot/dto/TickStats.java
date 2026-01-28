package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成交統計
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickStats {

    /**
     * 股票代號
     */
    private String symbol;

    /**
     * 股票名稱
     */
    private String name;

    /**
     * 總成交筆數
     */
    private Integer totalTicks;

    /**
     * 總成交量（股）
     */
    private Long totalVolume;

    /**
     * 外盤量（股）
     */
    private Long buyVolume;

    /**
     * 內盤量（股）
     */
    private Long sellVolume;

    /**
     * 外盤比例 %
     */
    private Double buyRatio;

    /**
     * 均價
     */
    private BigDecimal avgPrice;

    /**
     * 最新價
     */
    private BigDecimal lastPrice;

    /**
     * 最高價
     */
    private BigDecimal highPrice;

    /**
     * 最低價
     */
    private BigDecimal lowPrice;

    /**
     * 開盤價
     */
    private BigDecimal openPrice;

    /**
     * 昨收價
     */
    private BigDecimal prevClose;

    /**
     * 漲跌
     */
    private BigDecimal change;

    /**
     * 漲跌幅 %
     */
    private Double changePercent;

    /**
     * 更新時間
     */
    private LocalDateTime updateTime;

    /**
     * 資料來源
     */
    private String source;

    /**
     * 取得總成交張數
     */
    public long getTotalVolumeLots() {
        return totalVolume != null ? totalVolume / 1000 : 0;
    }

    /**
     * 取得外盤張數
     */
    public long getBuyVolumeLots() {
        return buyVolume != null ? buyVolume / 1000 : 0;
    }

    /**
     * 取得內盤張數
     */
    public long getSellVolumeLots() {
        return sellVolume != null ? sellVolume / 1000 : 0;
    }

    /**
     * 建立空的統計
     */
    public static TickStats empty(String symbol) {
        return TickStats.builder()
                .symbol(symbol)
                .totalTicks(0)
                .totalVolume(0L)
                .buyVolume(0L)
                .sellVolume(0L)
                .buyRatio(0.0)
                .build();
    }
}
