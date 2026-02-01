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
@Schema(description = "集保分散表單一級距資料")
public class ShareholdingDistributionData {

    @Schema(description = "股票代號")
    private String symbol;

    @Schema(description = "資料日期")
    private LocalDate date;

    @Schema(description = "持股級距")
    private String holdingSharesLevel;

    @Schema(description = "人數")
    private Long people;

    @Schema(description = "佔集保庫存比例(%)")
    private BigDecimal percent;

    @Schema(description = "股數")
    private Long unit;
}
