package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="selected_invoice_entity")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SelectedInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String templateName;
    private String username;
    private String updatedBy;
    private LocalDateTime updatedDate;
}