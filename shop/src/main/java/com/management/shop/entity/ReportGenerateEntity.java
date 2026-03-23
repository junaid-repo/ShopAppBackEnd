package com.management.shop.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportGenerateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	private String reportName;
    private String reportType;
	private String duration;
	private String createdDate;
    private LocalDate  startDate;
    private LocalDate endDate;
    private String status;
    private String format;
	private String fileName;
    private String userId;
}
