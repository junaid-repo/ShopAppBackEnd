package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoalData {

    private Double actualSales;
    private Double estimatedSales;
    private Integer actualProfit;
    private Integer estimatedProfit;
    private String fromDate;
    private String toDate;
}
