package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductRequest {
	
	private Integer selectedProductId;
	private String name;
	private String category;
	private Integer price;
    private Integer costPrice;
	private Integer stock;
	private Integer tax;
    private String hsn;
}
