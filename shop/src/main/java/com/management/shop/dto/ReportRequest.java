package com.management.shop.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportRequest {
	

private String reportType;
private String reportId;
private String format;
private String fromDate;
private String toDate;
	
}
