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
    private double discountPercentage; // e.g., 10.0 for 10% discount
    private double totalAmount;
    private double cgst;
    private Integer cgstPercentage;
    private double sgst;
    private Integer sgstPercentage;
    private double igst;
    private Integer igstPercentage;// Total for this line (rate * quantity + tax)

}
