package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind TaiwanStockInfo API Response
 */
@Data
public class TaiwanStockInfo {

    private Integer status;

    @JsonProperty("msg")
    private String message;

    private List<StockData> data;

    @Data
    public static class StockData {
        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("stock_name")
        private String stockName;

        @JsonProperty("industry_category")
        private String industryCategory;

        private String date;
    }
}
