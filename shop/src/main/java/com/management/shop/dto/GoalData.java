package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoalData implements Serializable {

    private Double actualSales;
    private Double estimatedSales;
    private Integer actualProfit;
    private Integer estimatedProfit;
    private String fromDate;
    private String toDate;
}
