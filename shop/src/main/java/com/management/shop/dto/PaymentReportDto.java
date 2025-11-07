package com.management.shop.dto;

import java.time.LocalDateTime;

public interface PaymentReportDto {
    String getPaymentReferenceNumber();
    String getInvoiceNumber();
    LocalDateTime getCreatedDate();
    String getPaymentMethod();
    Double getTotal();
    Double getPaid();
    Double getToBePaid();
    String getStatus();
}
