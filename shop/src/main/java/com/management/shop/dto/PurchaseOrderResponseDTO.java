package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponseDTO {
    private Integer id;
    private String poNumber;
    private Integer supplierId;
    private String supplierName;
    private String supplierPhone;
    private String supplierGstin;
    private String orderStatus;
    private String paymentStatus;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime receivedDate;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private String supplierInvoiceNumber;
    private String notes;
    private String billingAddress;
    private String bankDetails;
    private String paymentMode;
    private String signatureUrl;
    private List<PurchaseOrderItemResponseDTO> items;
    private Integer itemCount;
    private LocalDateTime createdDate;
}
