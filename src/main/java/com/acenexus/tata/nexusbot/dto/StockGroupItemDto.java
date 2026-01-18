package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockGroupItemDto {
    private Long id;
    private String stockSymbol;
    private String stockName;
    private Integer displayOrder;
}
