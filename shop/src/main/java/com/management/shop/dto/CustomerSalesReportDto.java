package com.management.shop.dto;

public interface CustomerSalesReportDto {
    String getName();
    String getEmail();
    String getPhone();
    Double getTotalSalesValue();
    Integer getOrderCount();
    String getInvoiceList();
}
