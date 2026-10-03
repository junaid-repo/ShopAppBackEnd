package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceivePurchaseOrderRequestDTO {
    private String supplierInvoiceNumber;
    private Boolean updateProductCostPrice; // Whether to update product's costPrice in catalog
    private List<ItemReceiveDetail> receivedItems;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemReceiveDetail {
        private Integer itemId; // purchase_order_items id
        private Integer quantityReceived;
        private BigDecimal unitPurchasePrice;
    }
}
