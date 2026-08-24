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
    private double cgst;
    private Double cgstPercentage;
    private double sgst;
    private Double sgstPercentage;
    private double igst;
    private Double igstPercentage;
    private String hsn;
    private Double discount;
}
