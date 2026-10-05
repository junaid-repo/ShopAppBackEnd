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
public class PurchaseOrderItemDTO {
    private Integer productId;
    private String productName;
    private String productBarcode;
    private Integer orderedQuantity;
    private Integer receivedQuantity;
    private BigDecimal unitPurchasePrice;
    private BigDecimal taxRate;
    private BigDecimal lineTotal;
}
