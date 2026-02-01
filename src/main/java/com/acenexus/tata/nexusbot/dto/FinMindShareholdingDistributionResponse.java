package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind 集保分散表 API Response
 * API: TaiwanStockHoldingSharesPer
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinMindShareholdingDistributionResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("data")
    private List<ShareholdingDistributionData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShareholdingDistributionData {
        @JsonProperty("date")
        private String date;

        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("HoldingSharesLevel")
        private String holdingSharesLevel;

        @JsonProperty("people")
        private Long people;

        @JsonProperty("percent")
        private Double percent;

        @JsonProperty("unit")
        private Long unit;
    }
}
