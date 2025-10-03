package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSalesReport {
    private String productName;
    private String category;
    private Long totalSold;
    private Double total;
    private Double tax;
    private Double profitOnCp;
    private String invoiceNumber;


}