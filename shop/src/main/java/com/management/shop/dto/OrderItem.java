package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItem {
    private String productName;
    private int quantity;
    private double unitPrice;
    private String details;
    private double gst;
    private Integer cgst;
    private Integer cgstPercentage;
    private Integer sgst;
    private Integer sgstPercentage;
    private Integer igst;
    private Integer igstPercentage;
    private String hsn;
}
