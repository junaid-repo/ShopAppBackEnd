package com.management.shop.gobalusers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AnalyticsResponse {
	private List<String> labels;
	private List<Long> sales;
	private List<Long> stocks;
	private List<Integer> taxes;
	private List<Integer> customers;
	private List<Long> profits;
	private List<Integer> onlinePayments;

	
}
