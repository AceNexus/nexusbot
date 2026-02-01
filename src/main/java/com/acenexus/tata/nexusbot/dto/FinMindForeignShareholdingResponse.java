package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind 外資持股 API Response
 * API: TaiwanStockShareholding
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinMindForeignShareholdingResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("data")
    private List<ForeignShareholdingData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForeignShareholdingData {
        @JsonProperty("date")
        private String date;

        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("stock_name")
        private String stockName;

        @JsonProperty("ForeignInvestmentShares")
        private Long foreignInvestmentShares;

        @JsonProperty("ForeignInvestmentRemainingShares")
        private Long foreignInvestmentRemainingShares;

        @JsonProperty("ForeignInvestmentSharesRatio")
        private Double foreignInvestmentSharesRatio;

        @JsonProperty("ForeignInvestmentRemainRatio")
        private Double foreignInvestmentRemainRatio;

        @JsonProperty("ForeignInvestmentUpperLimitRatio")
        private Double foreignInvestmentUpperLimitRatio;

        @JsonProperty("NumberOfSharesIssued")
        private Long numberOfSharesIssued;
    }
}
