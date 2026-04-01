package com.management.shop.gobalusers.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceSequenceId implements Serializable {

    private String shopId;
    private String financialYear;


}