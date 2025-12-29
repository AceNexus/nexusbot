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
 * K線數據（包含均線）
 * Candlestick Data with Moving Averages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "K線數據（包含均線）")
public class CandlestickWithMA {

    @Schema(description = "股票代號", example = "2330")
    private String symbol;

    @Schema(description = "日期", example = "2024-12-29")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "開盤價", example = "550.00")
    private BigDecimal open;

    @Schema(description = "最高價", example = "555.00")
    private BigDecimal high;

    @Schema(description = "最低價", example = "548.00")
    private BigDecimal low;

    @Schema(description = "收盤價", example = "553.00")
    private BigDecimal close;

    @Schema(description = "成交量（張）", example = "25000")
    private Long volume;

    @Schema(description = "成交金額（千元）", example = "13825000")
    private BigDecimal turnover;

    @Schema(description = "漲跌金額", example = "3.00")
    private BigDecimal change;

    @Schema(description = "漲跌幅 (%)", example = "0.55")
    private BigDecimal changePercent;

    // 均線數據
    @Schema(description = "5日均線", example = "551.20")
    private BigDecimal ma5;

    @Schema(description = "10日均線", example = "549.80")
    private BigDecimal ma10;

    @Schema(description = "20日均線", example = "545.50")
    private BigDecimal ma20;

    @Schema(description = "60日均線", example = "540.30")
    private BigDecimal ma60;
}
