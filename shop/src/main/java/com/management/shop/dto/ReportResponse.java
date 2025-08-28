package com.management.shop.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {

	private Integer id;

	private String name;

	private LocalDate fromDate;

	private LocalDate toDate;

	private OffsetDateTime createdAt;

	private String fileName;

	private String status;
}
