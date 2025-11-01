package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name="BillingPaymentsHistory")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentHistory {



    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private Integer billingId;
    private Integer paymentId;
    private Double paidAmount;
    private String tokenNo;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String updatedBy;
    private String userId;
    private String orderNumber;


}
