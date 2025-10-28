package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {
	
	private String id;
	private String saleId;
	private String date;
	private Double amount;
	private String method;
    private Double paid;
    private Double due;
    private String status;
    private Integer reminderCount;

}
