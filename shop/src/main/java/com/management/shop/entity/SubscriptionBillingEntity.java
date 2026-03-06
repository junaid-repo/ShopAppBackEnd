package com.management.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
public class SubscriptionBillingEntity {

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Integer id;
    private String name;
    private String phone;
    private String email;
    private String pincode;
    private String city;
    private String state;
    private String address;

    private String subsriptionPaymentId;
    private Boolean subscriptionStatus;
    private String username;


    private LocalDateTime updatedDate;
    private LocalDateTime createdDate;
    private String updatedBy;
    private String createdBy;
}
