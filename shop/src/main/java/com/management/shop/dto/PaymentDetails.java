package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails implements Serializable {
	
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
