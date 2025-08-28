package com.management.shop.dto;

import java.util.List;

import com.management.shop.entity.CustomerEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingRequest {
	private CustomerEntity selectedCustomer;
	private List<ProductBillDTO> cart;
	private Integer tax;
	//private Long subTotal;
	private Integer total;
    private Double discountPercentage;
    private String remarks;
	private String paymentMethod;
	

}
