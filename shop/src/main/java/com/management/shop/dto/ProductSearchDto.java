package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductSearchDto {
    private Long id;
    private String name;
    BigDecimal price;
    BigDecimal costPrice;
    int tax; // Assuming tax is a whole number percentage
    int stock;
    String hsn;
}