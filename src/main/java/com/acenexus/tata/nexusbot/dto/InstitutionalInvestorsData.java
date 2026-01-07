package com.acenexus.tata.nexusbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 法人進出數據
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "法人進出數據")
public class InstitutionalInvestorsData {

    @Schema(description = "股票代號", example = "2330")
    private String symbol;

    @Schema(description = "日期", example = "2024-12-30")
    private LocalDate date;

    // 外資
    @Schema(description = "外資買進（張）", example = "10000")
    private Long foreignInvestorBuy;

    @Schema(description = "外資賣出（張）", example = "5000")
    private Long foreignInvestorSell;

    @Schema(description = "外資買賣超（張）", example = "5000")
    private Long foreignInvestorBuySell;

    // 投信
    @Schema(description = "投信買進（張）", example = "3000")
    private Long investmentTrustBuy;

    @Schema(description = "投信賣出（張）", example = "2000")
    private Long investmentTrustSell;

    @Schema(description = "投信買賣超（張）", example = "1000")
    private Long investmentTrustBuySell;

    // 自營商
    @Schema(description = "自營商買進（張）", example = "2000")
    private Long dealerBuy;

    @Schema(description = "自營商賣出（張）", example = "2500")
    private Long dealerSell;

    @Schema(description = "自營商買賣超（張）", example = "-500")
    private Long dealerBuySell;

    // 三大法人合計
    @Schema(description = "三大法人買進合計（張）", example = "15000")
    private Long totalBuy;

    @Schema(description = "三大法人賣出合計（張）", example = "9500")
    private Long totalSell;

    @Schema(description = "三大法人買賣超合計（張）", example = "5500")
    private Long totalBuySell;

    /**
     * 判斷是否為買超
     */
    public boolean isForeignBuying() {
        return foreignInvestorBuySell != null && foreignInvestorBuySell > 0;
    }

    public boolean isInvestmentTrustBuying() {
        return investmentTrustBuySell != null && investmentTrustBuySell > 0;
    }

    public boolean isDealerBuying() {
        return dealerBuySell != null && dealerBuySell > 0;
    }

    public boolean isTotalBuying() {
        return totalBuySell != null && totalBuySell > 0;
    }
}
