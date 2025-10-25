package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class InvoiceData {

    // Shop Details
    private  String shopName;
    private  String shopSlogan;
    private  byte[] shopLogoBytes;
    private  String shopLogoText;
    private  String shopAddress;
    private  String shopEmail;
    private  String shopPhone;
    private  String gstNumber;
    private  String panNumber;


    // Invoice Details
    private  String invoiceId;
    private  String orderedDate;
    private  String dueDate;

    // Customer Details
    private  String customerName;
    private  String customerBillingAddress;
    private  String customerShippingAddress;
    private  String customerPhone;
    private  String customerState;
    private  String customerGst;

    // Items
    private List<OrderItemInvoice> products;

    // Financial Details
    private  double receivedAmount;
    private  double previousBalance;
    private double dueAmount;
    private double paidAmount;
    private double grandTotal;
    private double discountPercentage;
    private double discountAmount;
    List<Map<String, Object>> gstSummary = new ArrayList<>();

    // Bank & Payment Details
    private  String bankAccountName;
    private  String bankAccountNumber;
    private  String bankIfscCode;
    private  String bankName;
    private  String upiId;

    // Footer
    private  List<String> termsAndConditions;

    //Conditions

    Boolean printShopPan;

    Boolean printCustomerGst;
    Boolean combineCustomerAddresses;

    Boolean itemDiscount;
    Boolean showHsnColumn;
    Boolean showRateColumn;

    Boolean showTotalDiscount;
    Boolean printDueAmount;
    Boolean addDueDate;

    Boolean showSupportInfo;
    Boolean removeTerms;
}
