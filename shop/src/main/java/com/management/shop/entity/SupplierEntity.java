package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "shop_supplier", indexes = {
    @Index(name = "idx_supplier_user_id", columnList = "user_id"),
    @Index(name = "idx_supplier_phone", columnList = "phone")
})
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "company_name", length = 180)
    private String companyName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "upi_id", length = 80)
    private String upiId;

    @Builder.Default
    @Column(name = "balance_due", precision = 12, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
