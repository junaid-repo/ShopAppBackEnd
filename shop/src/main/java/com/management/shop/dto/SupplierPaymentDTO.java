package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPaymentDTO {
    private Integer id;
    private Integer supplierId;
    private String supplierName;
    private Integer purchaseOrderId;
    private String poNumber;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMode;
    private String transactionReference;
    private String remarks;
    private LocalDateTime createdAt;
}
