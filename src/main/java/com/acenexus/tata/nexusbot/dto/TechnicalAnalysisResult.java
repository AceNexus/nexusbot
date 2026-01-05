package com.acenexus.tata.nexusbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 技術分析結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "技術分析結果")
public class TechnicalAnalysisResult {

    @Schema(description = "股票代號", example = "2330")
    private String symbol;

    @Schema(description = "股票名稱", example = "台積電")
    private String name;

    @Schema(description = "分析日期", example = "2024-12-30")
    private LocalDate analysisDate;

    @Schema(description = "當前收盤價", example = "585.00")
    private BigDecimal currentPrice;

    @Schema(description = "綜合建議", example = "買進")
    private String recommendation;  // 買進、賣出、持倉、觀望

    @Schema(description = "信心指數 (0-100)", example = "75")
    private Integer confidence;

    @Schema(description = "技術指標分析")
    private TechnicalIndicators indicators;

    @Schema(description = "買賣訊號列表")
    private List<TradingSignal> signals;

    @Schema(description = "支撐壓力位")
    private SupportResistance supportResistance;

    @Schema(description = "進出場時機分析")
    private EntryExitTiming entryExitTiming;

    @Schema(description = "K線型態詳情列表")
    private List<CandlestickPatternDetail> candlestickPatternDetails;

    @Schema(description = "趨勢描述", example = "強勢上升趨勢")
    private String trendDescription;

    @Schema(description = "量價關係描述", example = "價漲量增，多頭強勢")
    private String priceVolumeAnalysis;

    @Schema(description = "分析說明")
    private String analysis;

    /**
     * 技術指標
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalIndicators {
        @Schema(description = "RSI (6日)", example = "68.5")
        private BigDecimal rsi6;

        @Schema(description = "RSI (12日)", example = "65.2")
        private BigDecimal rsi12;

        @Schema(description = "KD - K值", example = "75.5")
        private BigDecimal kdK;

        @Schema(description = "KD - D值", example = "72.3")
        private BigDecimal kdD;

        @Schema(description = "MACD - DIF", example = "2.5")
        private BigDecimal macdDif;

        @Schema(description = "MACD - DEA", example = "1.8")
        private BigDecimal macdDea;

        @Schema(description = "MACD - 柱狀圖", example = "1.4")
        private BigDecimal macdHistogram;

        @Schema(description = "布林帶 - 上軌", example = "595.00")
        private BigDecimal bbandsUpper;

        @Schema(description = "布林帶 - 中軌", example = "585.00")
        private BigDecimal bbandsMiddle;

        @Schema(description = "布林帶 - 下軌", example = "575.00")
        private BigDecimal bbandsLower;

        @Schema(description = "MA5", example = "583.00")
        private BigDecimal ma5;

        @Schema(description = "MA10", example = "580.00")
        private BigDecimal ma10;

        @Schema(description = "MA20", example = "575.00")
        private BigDecimal ma20;

        @Schema(description = "MA60", example = "570.00")
        private BigDecimal ma60;

        @Schema(description = "Williams %R (14日)", example = "-35.50")
        private BigDecimal williamsR14;

        @Schema(description = "BIAS (6日)", example = "2.50")
        private BigDecimal bias6;

        @Schema(description = "BIAS (12日)", example = "1.80")
        private BigDecimal bias12;

        @Schema(description = "BIAS (24日)", example = "0.95")
        private BigDecimal bias24;
    }

    /**
     * 交易訊號
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingSignal {
        @Schema(description = "訊號類型", example = "黃金交叉")
        private String type;

        @Schema(description = "訊號方向", example = "買進")
        private String direction;  // 買進、賣出

        @Schema(description = "強度 (1-5)", example = "4")
        private Integer strength;

        @Schema(description = "說明", example = "MA5 向上穿越 MA10，形成黃金交叉")
        private String description;

        @Schema(description = "觸發日期", example = "2024-12-30")
        private LocalDate date;
    }

    /**
     * 支撐壓力位
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportResistance {
        @Schema(description = "第一支撐位", example = "575.00")
        private BigDecimal support1;

        @Schema(description = "第二支撐位", example = "570.00")
        private BigDecimal support2;

        @Schema(description = "第一壓力位", example = "590.00")
        private BigDecimal resistance1;

        @Schema(description = "第二壓力位", example = "595.00")
        private BigDecimal resistance2;
    }

    /**
     * 進出場時機分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryExitTiming {
        @Schema(description = "操作建議", example = "進場")
        private String action;  // 進場、出場、觀望、加碼、減碼

        @Schema(description = "建議買入價位（若為進場建議）", example = "580.00")
        private BigDecimal entryPrice;

        @Schema(description = "建議賣出價位（若為出場建議）", example = "590.00")
        private BigDecimal exitPrice;

        @Schema(description = "最佳買入價位（保守）", example = "575.00")
        private BigDecimal entryPriceMin;

        @Schema(description = "可接受買入價位（積極）", example = "585.00")
        private BigDecimal entryPriceMax;

        @Schema(description = "可接受賣出價位（保守）", example = "590.00")
        private BigDecimal exitPriceMin;

        @Schema(description = "最佳賣出價位（積極）", example = "600.00")
        private BigDecimal exitPriceMax;

        @Schema(description = "停損價位", example = "570.00")
        private BigDecimal stopLossPrice;

        @Schema(description = "第一停利價位", example = "600.00")
        private BigDecimal takeProfit1;

        @Schema(description = "第二停利價位", example = "610.00")
        private BigDecimal takeProfit2;

        @Schema(description = "風險報酬比", example = "1:2.5")
        private String riskRewardRatio;

        @Schema(description = "分析與建議（整合技術分析依據與操作建議）")
        private String analysisDetail;

        @Schema(description = "建議部位大小 (%)", example = "30")
        private Integer positionSize;
    }

    /**
     * K 線型態詳情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandlestickPatternDetail {
        @Schema(description = "型態名稱", example = "看漲吞沒")
        private String name;

        @Schema(description = "詳細說明", example = "大陽線完全吞沒前一根陰線，表示多方力量強勢反攻...")
        private String description;

        @Schema(description = "可靠度", example = "高")
        private String reliability;

        @Schema(description = "看漲/看跌/中性", example = "true")
        private Boolean bullish;

        @Schema(description = "出現日期", example = "2024-12-30")
        private LocalDate date;
    }
}
