package com.management.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_sequences")
@IdClass(InvoiceSequenceId.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceSequence {

    @Id
    @Column(name = "shop_id", length = 50, nullable = false)
    private String shopId;

    @Id
    @Column(name = "financial_year", length = 10, nullable = false)
    private String financialYear;

    @Column(name = "prefix", length = 20, nullable = false)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private Integer currentValue = 0;

    private String username;
    private LocalDateTime updatedDate;

}