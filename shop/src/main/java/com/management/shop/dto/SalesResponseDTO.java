package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SalesResponseDTO {
	
	private String id;
	private String customer;
	private String date;
	private Double total;
    private String remarks;
	private String 	status;
    private Double paid;
    private String gstin;
    private Integer reminderCount;
	
}
