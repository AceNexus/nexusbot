package com.acenexus.tata.nexusbot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 技術指標數據
 * Technical Indicator Data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "技術指標數據")
public class TechnicalIndicatorData {

    @Schema(description = "日期", example = "2024-12-29")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "5日移動平均線", example = "550.00")
    private BigDecimal ma5;

    @Schema(description = "20日移動平均線（月線）", example = "545.00")
    private BigDecimal ma20;

    @Schema(description = "60日移動平均線（季線）", example = "540.00")
    private BigDecimal ma60;

    @Schema(description = "相對強弱指標 RSI(14)", example = "65.50")
    private BigDecimal rsi;

    @Schema(description = "月線乖離率 (%)", example = "2.35")
    private BigDecimal ma20Bias;

    @Schema(description = "是否站上月線", example = "true")
    private Boolean aboveMA20;
}
