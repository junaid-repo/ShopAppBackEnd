package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequestDTO {
    private Integer supplierId;
    private String orderStatus; // "DRAFT" or "ORDERED"
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String supplierInvoiceNumber;
    private String notes;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String billingAddress;
    private String bankDetails;
    private String paymentMode;
    private String signatureUrl;
    private List<PurchaseOrderItemDTO> items;
}
