package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="Shop_inovice_terms")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopInvoiceTermsEnity {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private String userId;
    private Integer shopId;
    private String term;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
