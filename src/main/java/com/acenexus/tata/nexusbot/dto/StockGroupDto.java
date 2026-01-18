package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockGroupDto {
    private Long id;
    private String name;
    private Integer displayOrder;
    private Boolean isSelected;
    private List<StockGroupItemDto> stocks;
}
