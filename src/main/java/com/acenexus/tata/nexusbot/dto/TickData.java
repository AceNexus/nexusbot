package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 單筆成交資料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickData {

    /**
     * 股票代號
     */
    private String symbol;

    /**
     * 成交時間
     */
    private LocalDateTime time;

    /**
     * 成交價
     */
    private BigDecimal price;

    /**
     * 成交量（股）
     */
    private Integer volume;

    /**
     * 成交序號
     */
    private Long serial;

    /**
     * 最佳買價
     */
    private BigDecimal bidPrice;

    /**
     * 最佳賣價
     */
    private BigDecimal askPrice;

    /**
     * 內盤/外盤類型
     */
    private TickType tickType;

    /**
     * 資料來源
     */
    private String source;

    /**
     * 取得成交張數
     */
    public int getVolumeLots() {
        return volume != null ? volume / 1000 : 0;
    }
}
