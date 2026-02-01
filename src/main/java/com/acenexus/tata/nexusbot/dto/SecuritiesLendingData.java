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
@Schema(description = "借券每日彙總資料")
public class SecuritiesLendingData {

    @Schema(description = "股票代號")
    private String symbol;

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "交易方式（議借/競價）")
    private String transactionType;

    @Schema(description = "成交股數合計")
    private Long totalVolume;

    @Schema(description = "成交筆數")
    private Integer totalTransactions;

    @Schema(description = "平均費率(%)")
    private BigDecimal avgFeeRate;

    @Schema(description = "收盤價")
    private BigDecimal closePrice;
}
