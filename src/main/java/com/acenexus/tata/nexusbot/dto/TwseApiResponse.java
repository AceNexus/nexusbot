package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 台灣證交所 API 回應
 * API: https://mis.twse.com.tw/stock/api/getStockInfo.jsp
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwseApiResponse {

    @JsonProperty("msgArray")
    private List<TwseStockData> stockDataList;

    @JsonProperty("rtcode")
    private String returnCode;

    @JsonProperty("rtmessage")
    private String returnMessage;

    /**
     * 檢查回應是否成功
     */
    public boolean isSuccess() {
        return "0000".equals(returnCode) && stockDataList != null && !stockDataList.isEmpty();
    }

    /**
     * 台灣證交所股票資料
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwseStockData {
        /**
         * 股票代號
         */
        @JsonProperty("c")
        private String symbol;

        /**
         * 股票名稱
         */
        @JsonProperty("n")
        private String name;

        /**
         * 當前價格（盤中最新成交價或收盤價）
         * 盤後顯示 "-"
         */
        @JsonProperty("z")
        private String currentPrice;

        /**
         * 開盤價
         */
        @JsonProperty("o")
        private String openPrice;

        /**
         * 最高價
         */
        @JsonProperty("h")
        private String highPrice;

        /**
         * 最低價
         */
        @JsonProperty("l")
        private String lowPrice;

        /**
         * 昨收價
         */
        @JsonProperty("y")
        private String previousClose;

        /**
         * 成交量（單位: 張，1張 = 1000股）
         */
        @JsonProperty("v")
        private String volume;

        /**
         * 時間 (HH:mm:ss)
         */
        @JsonProperty("t")
        private String time;

        /**
         * 日期 (yyyyMMdd)
         */
        @JsonProperty("d")
        private String date;

        /**
         * 漲停價
         */
        @JsonProperty("u")
        private String limitUp;

        /**
         * 跌停價
         */
        @JsonProperty("w")
        private String limitDown;

        /**
         * 最佳買價
         */
        @JsonProperty("ob")
        private String bestBidPrice;

        /**
         * 最佳賣價
         */
        @JsonProperty("oa")
        private String bestAskPrice;

        /**
         * 成交金額（百萬）
         */
        @JsonProperty("tv")
        private String turnover;
    }
}
