package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="Shop_UPI")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopUPIEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private String userId;
    private Integer shopId;
    private Integer shopFinanceId;
    private String upiId;
    private String upiProvider;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
