package com.management.shop.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="Shop_Banks")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopBankEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private String userId;
    private Integer shopId;
    private Integer shopFinanceId;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
