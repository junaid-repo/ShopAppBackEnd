package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DasbboardResponseDTO {
	
    private Integer monthlyRevenue;
    private Integer taxCollected;
    private Integer totalUnitsSold;
    private Integer outOfStockCount;
    private Integer countOfSales;
    private String heatmapGranularity;
    private List<SalesHeatmapPointDTO> salesHeatmap;

}
