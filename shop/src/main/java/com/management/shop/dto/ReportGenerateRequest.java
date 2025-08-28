package com.management.shop.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportGenerateRequest {
	
	private String reportType;
	private LocalDateTime fromDate;
	private LocalDateTime toDate;

}
