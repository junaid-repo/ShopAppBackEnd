package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSummaryDTO {
    private BigDecimal totalPurchases;
    private Integer totalOrdersCount;
    private Integer pendingOrdersCount;
    private BigDecimal totalDueToSuppliers;
}
