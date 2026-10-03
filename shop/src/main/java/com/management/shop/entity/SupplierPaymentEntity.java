package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "supplier_payments", indexes = {
    @Index(name = "idx_sup_pay_user_id", columnList = "user_id"),
    @Index(name = "idx_sup_pay_supplier_id", columnList = "supplier_id"),
    @Index(name = "idx_sup_pay_date", columnList = "payment_date")
})
public class SupplierPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = true)
    private PurchaseOrderEntity purchaseOrder;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_mode", length = 30) // CASH, UPI, BANK_TRANSFER, CHEQUE
    private String paymentMode;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(length = 255)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
