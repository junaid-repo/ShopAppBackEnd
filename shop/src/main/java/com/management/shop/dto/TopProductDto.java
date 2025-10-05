package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductDto {

    private String productName;
    private String category;
    private long count; // Represents units sold
    private double amount; // Represents total revenue
    private int currentStock;

}