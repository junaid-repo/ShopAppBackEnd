package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductDto implements Serializable {

    private String productName;
    private String category;
    private long count; // Represents units sold
    private double amount; // Represents total revenue
    private int currentStock;

}