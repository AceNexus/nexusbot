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
@Schema(description = "集保分散彙總（大戶/散戶比例）")
public class ShareholdingDistributionSummary {

    @Schema(description = "股票代號")
    private String symbol;

    @Schema(description = "資料日期")
    private LocalDate date;

    @Schema(description = "總人數")
    private Long totalPeople;

    @Schema(description = "總股數")
    private Long totalUnit;

    @Schema(description = "大戶持股比例(%) - 持股 400張以上")
    private BigDecimal majorHolderPercent;

    @Schema(description = "散戶持股比例(%) - 持股 400張以下")
    private BigDecimal retailHolderPercent;

    @Schema(description = "大戶人數")
    private Long majorHolderPeople;

    @Schema(description = "散戶人數")
    private Long retailHolderPeople;
}
