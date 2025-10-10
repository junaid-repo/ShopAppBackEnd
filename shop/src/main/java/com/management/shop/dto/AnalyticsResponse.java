package com.management.shop.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AnalyticsResponse {
	private List<String> labels;
	private List<Long> revenues;
	private List<Long> stocks;
	private List<Integer> taxes;
	private List<Integer> customers;
	private List<Long> profits;
	private List<Integer> sales;

	
}
