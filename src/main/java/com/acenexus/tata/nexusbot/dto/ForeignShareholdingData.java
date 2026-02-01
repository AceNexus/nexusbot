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
@Schema(description = "外資持股每日資料")
public class ForeignShareholdingData {

    @Schema(description = "股票代號")
    private String symbol;

    @Schema(description = "股票名稱")
    private String name;

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "外資持股數")
    private Long foreignInvestmentShares;

    @Schema(description = "外資尚可投資股數")
    private Long foreignInvestmentRemainingShares;

    @Schema(description = "外資持股比例(%)")
    private BigDecimal foreignInvestmentSharesRatio;

    @Schema(description = "外資尚可投資比例(%)")
    private BigDecimal foreignInvestmentRemainRatio;

    @Schema(description = "外資投資上限比例(%)")
    private BigDecimal foreignInvestmentUpperLimitRatio;

    @Schema(description = "發行股數")
    private Long numberOfSharesIssued;
}
