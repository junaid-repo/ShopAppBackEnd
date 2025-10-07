package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="Shop_finance")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopFinanceEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private String userId;
    private String gstin;
    private String panNumber;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
