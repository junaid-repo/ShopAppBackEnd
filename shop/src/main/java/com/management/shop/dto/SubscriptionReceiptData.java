package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionReceiptData {

    private String appName = "Clear Bill";
    private String appAddress = "123 Business Avenue, Ranchi, JH 834001";
    private String appGstin = "20ABCDE1234F1Z5";
    private String appPhone = "+91 98765 43210";
    private String appEmail = "support@clearbill.com";

    // 2. Invoice Details
    private String invoiceId; // e.g., "SUB-20251104-0402"
    private String invoiceDate; // e.g., "04 Nov 2025"

    // 3. User Details (Bill To)
    private String userName;
    private String userEmail;
    private String userAddress; // Combined billing/shipping address
    private String userGstin; // User's GSTIN (if they provided one)
    private String userPhone;

    // 4. Line Item & 5. Summary (Combined)
    // We only have one "product"
    private String planName;      // e.g., "Premium - Monthly Plan"
    private BigDecimal taxableAmount; // e.g., 168.64
    private Map<String, BigDecimal> gstSummary; // e.g., {"CGST @9%": 15.18, "SGST @9%": 15.18}
    private BigDecimal totalGstAmount; // e.g., 30.36
    private BigDecimal totalAmount;    // e.g., 199.00
    private String amountInWords;
}
