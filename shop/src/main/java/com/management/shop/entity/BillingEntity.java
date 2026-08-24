package com.management.shop.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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
	@Column(columnDefinition = "DECIMAL(19,2)")
	private Double taxAmount;
	@Column(columnDefinition = "DECIMAL(19,2)")
	private Double subTotalAmount;
	@Column(columnDefinition = "DECIMAL(19,2)")
	private Double totalAmount;
    @Column(columnDefinition = "DECIMAL(7,4)")
    private Double discountPercent;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double discountAmount;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double cgstAmount;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double sgstAmount;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double igstAmount;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double payingAmount;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double remainingAmount;
    private Integer customerId;
	private Integer unitsSold;
    private String remarks;
    private String gstin;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double totalProfitOnCP;
    private String invoiceStatus;
    private Integer dueReminderCount;
	private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private String updatedBy;
    private String userId;



}
