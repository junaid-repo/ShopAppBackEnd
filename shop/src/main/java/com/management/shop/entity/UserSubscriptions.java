package com.management.shop.entity;

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
public class UserSubscriptions {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String subscriptionId;
    private String planType;
    private Integer days;
    private Double price;

    private String status;
    private String username;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
