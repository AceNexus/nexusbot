package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind 融資融券 API Response
 * API: TaiwanStockMarginPurchaseShortSale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinMindMarginTradingResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("data")
    private List<MarginTradingData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MarginTradingData {
        @JsonProperty("date")
        private String date;

        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("MarginPurchaseBuy")
        private Long marginPurchaseBuy;

        @JsonProperty("MarginPurchaseSell")
        private Long marginPurchaseSell;

        @JsonProperty("MarginPurchaseCashRepayment")
        private Long marginPurchaseCashRepayment;

        @JsonProperty("MarginPurchaseYesterdayBalance")
        private Long marginPurchaseYesterdayBalance;

        @JsonProperty("MarginPurchaseTodayBalance")
        private Long marginPurchaseTodayBalance;

        @JsonProperty("MarginPurchaseLimit")
        private Long marginPurchaseLimit;

        @JsonProperty("ShortSaleBuy")
        private Long shortSaleBuy;

        @JsonProperty("ShortSaleSell")
        private Long shortSaleSell;

        @JsonProperty("ShortSaleCashRepayment")
        private Long shortSaleCashRepayment;

        @JsonProperty("ShortSaleYesterdayBalance")
        private Long shortSaleYesterdayBalance;

        @JsonProperty("ShortSaleTodayBalance")
        private Long shortSaleTodayBalance;

        @JsonProperty("ShortSaleLimit")
        private Long shortSaleLimit;

        @JsonProperty("OffsetLoanAndShort")
        private Long offsetLoanAndShort;

        @JsonProperty("Note")
        private String note;
    }
}
