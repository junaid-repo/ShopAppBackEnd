package com.management.shop.dto;

public interface PaymentSummaryDto {
    String getCategory(); // Will hold either 'method' or 'status'
    Double getTotalAmount();
    String getInvoiceList();
}
