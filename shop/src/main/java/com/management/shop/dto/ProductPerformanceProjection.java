package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public interface ProductPerformanceProjection {

    String getProductName();
    String getCategory();
    Long getUnitsSold();
    Double getRevenue();
    Integer getCurrentStock();
}
