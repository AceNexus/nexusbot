package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind 借券 API Response
 * API: TaiwanStockSecuritiesLending
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinMindSecuritiesLendingResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("data")
    private List<SecuritiesLendingData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SecuritiesLendingData {
        @JsonProperty("date")
        private String date;

        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("transaction_type")
        private String transactionType;

        @JsonProperty("volume")
        private Long volume;

        @JsonProperty("fee_rate")
        private Double feeRate;

        @JsonProperty("close")
        private Double close;
    }
}
