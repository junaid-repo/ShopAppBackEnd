package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {
    private Integer id;
    private String name;
    private String companyName;
    private String phone;
    private String email;
    private String gstin;
    private String address;
    private String upiId;
    private BigDecimal balanceDue;
    private Boolean isActive;
    private LocalDateTime createdDate;
}
