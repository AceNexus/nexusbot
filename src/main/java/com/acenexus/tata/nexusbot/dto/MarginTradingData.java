package com.acenexus.tata.nexusbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "融資融券每日資料")
public class MarginTradingData {

    @Schema(description = "股票代號")
    private String symbol;

    @Schema(description = "日期")
    private LocalDate date;

    // 融資
    @Schema(description = "融資買進")
    private Long marginPurchaseBuy;

    @Schema(description = "融資賣出")
    private Long marginPurchaseSell;

    @Schema(description = "融資現金償還")
    private Long marginPurchaseCashRepayment;

    @Schema(description = "融資前日餘額")
    private Long marginPurchaseYesterdayBalance;

    @Schema(description = "融資今日餘額")
    private Long marginPurchaseTodayBalance;

    @Schema(description = "融資限額")
    private Long marginPurchaseLimit;

    @Schema(description = "融資增減")
    private Long marginPurchaseChange;

    // 融券
    @Schema(description = "融券買進")
    private Long shortSaleBuy;

    @Schema(description = "融券賣出")
    private Long shortSaleSell;

    @Schema(description = "融券現金償還")
    private Long shortSaleCashRepayment;

    @Schema(description = "融券前日餘額")
    private Long shortSaleYesterdayBalance;

    @Schema(description = "融券今日餘額")
    private Long shortSaleTodayBalance;

    @Schema(description = "融券限額")
    private Long shortSaleLimit;

    @Schema(description = "融券增減")
    private Long shortSaleChange;

    // 其他
    @Schema(description = "資券互抵")
    private Long offsetLoanAndShort;

    @Schema(description = "融資使用率(%)")
    private BigDecimal utilizationRate;

    @Schema(description = "備註")
    private String note;
}
