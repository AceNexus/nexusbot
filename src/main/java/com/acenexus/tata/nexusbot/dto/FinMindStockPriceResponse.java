package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind 股價 API 回應
 * API: TaiwanStockPrice
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinMindStockPriceResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("data")
    private List<StockPriceData> data;

    /**
     * 股價數據
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockPriceData {

        @JsonProperty("date")
        private String date;  // yyyy-MM-dd

        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("Trading_Volume")
        private String tradingVolume;  // 成交股數

        @JsonProperty("Trading_money")
        private String tradingMoney;  // 成交金額

        @JsonProperty("open")
        private String open;  // 開盤價

        @JsonProperty("max")
        private String high;  // 最高價

        @JsonProperty("min")
        private String low;  // 最低價

        @JsonProperty("close")
        private String close;  // 收盤價

        @JsonProperty("spread")
        private String spread;  // 漲跌價差

        @JsonProperty("Trading_turnover")
        private String tradingTurnover;  // 成交筆數
    }
}
