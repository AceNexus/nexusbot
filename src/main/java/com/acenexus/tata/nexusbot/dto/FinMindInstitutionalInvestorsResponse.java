package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * FinMind 法人進出 API Response
 * API: https://api.finmindtrade.com/api/v4/data?dataset=TaiwanStockInstitutionalInvestorsBuySell
 * <p>
 * 資料格式說明：每一天會有多筆記錄，每筆記錄代表不同的法人類型
 * name 欄位可能的值：
 * - Foreign_Investor: 外資
 * - Investment_Trust: 投信
 * - Dealer_self: 自營商（自營）
 * - Dealer_Hedging: 自營商（避險）
 * - Foreign_Dealer_Self: 外資及陸資自營商
 */
@Data
public class FinMindInstitutionalInvestorsResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("data")
    private List<InstitutionalInvestorData> data;

    @Data
    public static class InstitutionalInvestorData {
        @JsonProperty("date")
        private String date;

        @JsonProperty("stock_id")
        private String stockId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("buy")
        private String buy;

        @JsonProperty("sell")
        private String sell;
    }
}
