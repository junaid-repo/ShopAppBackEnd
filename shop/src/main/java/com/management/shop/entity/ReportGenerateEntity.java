package com.management.shop.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportGenerateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	private String reportName;
	private String duration;
	private String createdDate;
	private String fileName;
    private String userId;
}
