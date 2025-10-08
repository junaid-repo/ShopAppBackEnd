package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemInvoice {
    private String productName;
    private String hsnCode; // NEW
    private int quantity;
    private String description;
    private double rate; // Price BEFORE tax
    private double taxAmount; // Total tax for this line item
    private double taxPercentage; // e.g., 18.0
    private double totalAmount;
    private Integer cgst;
    private Integer cgstPercentage;
    private Integer sgst;
    private Integer sgstPercentage;
    private Integer igst;
    private Integer igstPercentage;// Total for this line (rate * quantity + tax)

}
