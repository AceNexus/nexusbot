package com.acenexus.tata.nexusbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockSymbolDto {
    private String symbol;
    private String name;
}
