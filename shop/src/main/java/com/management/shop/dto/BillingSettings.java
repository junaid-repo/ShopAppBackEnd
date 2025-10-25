package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingSettings {

    private Boolean autoSendInvoice;
    private Boolean allowNoStockBilling;
    private Boolean hideNoStockProducts;
    private String serialNumberPattern;
    private Boolean showPartialPaymentOption;
    private Boolean showRemarksOnSummarySide;


}
