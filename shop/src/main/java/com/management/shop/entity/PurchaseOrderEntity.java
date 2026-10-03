package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "purchase_orders", indexes = {
    @Index(name = "idx_po_user_id", columnList = "user_id"),
    @Index(name = "idx_po_number", columnList = "po_number"),
    @Index(name = "idx_po_status", columnList = "order_status"),
    @Index(name = "idx_po_date", columnList = "order_date")
})
public class PurchaseOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "po_number", nullable = false, length = 50)
    private String poNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private PurchaseOrderStatus orderStatus = PurchaseOrderStatus.DRAFT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PurchasePaymentStatus paymentStatus = PurchasePaymentStatus.UNPAID;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Builder.Default
    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "due_amount", precision = 12, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    @Column(name = "supplier_invoice_number", length = 100)
    private String supplierInvoiceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "bank_details", columnDefinition = "TEXT")
    private String bankDetails;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseOrderItemEntity> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
