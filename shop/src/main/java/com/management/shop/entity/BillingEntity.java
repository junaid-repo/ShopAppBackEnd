package com.management.shop.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="BillingDetails")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BillingEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	private String invoiceNumber;
	private Double taxAmount;
	private Double subTotalAmount;
	private Double totalAmount;
    private Double discountPercent;
    private Double payingAmount;
    private Double remainingAmount;
    private Integer customerId;
	private Integer unitsSold;
    private String remarks;
    private String gstin;
    private Double totalProfitOnCP;
    private String invoiceStatus;
    private Integer dueReminderCount;
	private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private String updatedBy;
    private String userId;



}
