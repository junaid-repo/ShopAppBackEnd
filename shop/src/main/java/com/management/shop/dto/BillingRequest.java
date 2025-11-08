package com.management.shop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
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
	private Double tax;
	//private Long subTotal;
    @JsonProperty("sellingSubtotal")
	private Double total;
    private Double payingAmount;
    private Double remainingAmount;
    private String gstin;
    private Double discountPercentage;
    private String remarks;
	private String paymentMethod;
	

}
