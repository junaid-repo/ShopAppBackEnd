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
public class SubsriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String userSubId;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String gatewaySignature;
    private Double amount;

    private String username;
    private LocalDateTime paymentDate;
    private LocalDateTime updatedAt;
    private String updatedBy;

}
