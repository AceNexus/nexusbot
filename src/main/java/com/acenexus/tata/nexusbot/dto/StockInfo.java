package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


//{"msgArray":[{"@":"2330.tw","tv":"2346","ps":"3688","pid":"9.tse.tw|15455","pz":"1520.0000","bp":"0","fv":"118","oa":"1530.0000","ob":"1525.0000","m%":"000000","^":"20251229","key":"tse_2330.tw_20251229","a":"1530.0000_1535.0000_1540.0000_1545.0000_1550.0000_","b":"1525.0000_1520.0000_1515.0000_1510.0000_1505.0000_","c":"2330","#":"13.tse.tw|2467","d":"20251229","%":"14:30:00","ch":"2330.tw","tlong":"1766989800000","ot":"14:30:00","f":"3056_2072_1747_1077_2264_","g":"493_1686_1283_1393_1419_","ip":"0","mt":"000000","ov":"86648","h":"1530.0000","i":"24","it":"12","oz":"1525.0000","l":"1510.0000","n":"台積電","o":"1515.0000","p":"0","ex":"tse","s":"2346","t":"13:30:00","u":"1660.0000","v":"20920","w":"1360.0000","nf":"台灣積體電路製造股份有限公司","y":"1510.0000","z":"1530.0000","ts":"0"}],"referer":"","userDelay":5000,"rtcode":"0000","queryTime":{"sysDate":"20251229","stockInfoItem":4454,"stockInfo":489,"sessionStr":"UserSession","sysTime":"16:14:06","showChart":false,"sessionFromTime":1766989800000,"sessionLatestTime":1766989800000},"rtmessage":"OK","exKey":"if_tse_2330.tw_zh-tw.null","cachedAlive":7651},[Server:"nginx", Date:"Mon, 29 Dec 2025 08:14:13 GMT", Content-Type:"text/html;charset=UTF-8", Content-Length:"1159", Connection:"keep-alive", Vary:"Accept-Encoding", X-Frame-Options:"DENY", X-Content-Type-Options:"nosniff", Set-Cookie:"JSESSIONID=5F31EF5F5E9E827B52078AB06A668B55; Path=/; Secure; HttpOnly;stock; Secure; HttpOnly", Pragma:"no-cache", Cache-Control:"no-cache", "no-store, no-cache, must-revalidate", Expires:"Thu, 01 Jan 1970 00:00:00 GMT"]>


/**
 * 股票資訊 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockInfo {
    /**
     * 股票代號
     */
    private String symbol;

    /**
     * 股票名稱
     */
    private String name;

    /**
     * 當前價格
     */
    private BigDecimal currentPrice;

    /**
     * 開盤價
     */
    private BigDecimal openPrice;

    /**
     * 最高價
     */
    private BigDecimal highPrice;

    /**
     * 最低價
     */
    private BigDecimal lowPrice;

    /**
     * 昨收價
     */
    private BigDecimal previousClose;

    /**
     * 漲跌金額
     */
    private BigDecimal change;

    /**
     * 漲跌幅 (%)
     */
    private BigDecimal changePercent;

    /**
     * 成交量
     */
    private Long volume;

    /**
     * 更新時間
     */
    private LocalDateTime updateTime;

    /**
     * 漲停價
     */
    private BigDecimal limitUp;

    /**
     * 跌停價
     */
    private BigDecimal limitDown;

    /**
     * 最佳買價
     */
    private BigDecimal bestBidPrice;

    /**
     * 最佳賣價
     */
    private BigDecimal bestAskPrice;

    /**
     * 成交金額（百萬）
     */
    private BigDecimal turnover;
}