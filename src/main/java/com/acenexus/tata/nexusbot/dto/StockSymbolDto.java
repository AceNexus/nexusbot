package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSymbolDto {
    private String symbol;
    private String name;
    private String market;  // "上市" or "上櫃"

    public StockSymbolDto(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
        this.market = null;
    }
}
