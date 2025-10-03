package com.management.shop.dto;

import java.time.LocalDateTime;

public interface ProductSalesReportView {
    String getProductName();
    String getCategory();
    Long getTotalSold();
    Double getTotal();
    Double getTax();
    Double getProfitOnCp();
    String getInvoiceNumber();
    LocalDateTime getInvoiceDate();
}