package com.management.shop.gobalusers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Entity
@Table
public class UserSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private Boolean isDarkModeDefault;
    private Boolean isBillingPageDefault;
    private Boolean autoPrintInvoice;
    private Boolean lowStockAlert;
    private Integer autoDeleteNotification;
    private Boolean autoDeleteCustomers;
    private Integer autoDeleteCustomerForMinSpent;
    private Integer autoDeleteCustomerForInactiveDays;

   private Boolean autoSendInvoice;
    private Boolean allowNoStockBilling;
    private Boolean hideNoStockProducts;
    private String serialNumberPattern;

    private Boolean addDueDate;
    private Boolean combineAddresses;
    private Boolean showPaymentStatus;
    private Boolean removeTerms;
    private Boolean showCustomerGstin;

    private String username;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
