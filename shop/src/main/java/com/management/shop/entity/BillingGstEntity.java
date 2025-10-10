package com.management.shop.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="Billing_gst")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BillingGstEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    private Integer billingId;
    private String gstType; // CGST, SGST, IGST
    private Double gstPercentage;
    private Double gstAmount;
    private LocalDateTime updatedDate;
    private String updatedBy;
    private String userId;
    private String orderNumber;
}
