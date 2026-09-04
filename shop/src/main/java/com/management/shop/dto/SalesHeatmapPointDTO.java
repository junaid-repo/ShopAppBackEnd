package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesHeatmapPointDTO {

    private String key;
    private String label;
    private Double amount;
    private Integer salesCount;
}
