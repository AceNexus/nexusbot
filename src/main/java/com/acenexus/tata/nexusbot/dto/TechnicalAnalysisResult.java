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
}
