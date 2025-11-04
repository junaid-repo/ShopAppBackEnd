package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyAndBillRequest {
    public String razorpay_order_id;
    public String razorpay_payment_id;
    public String razorpay_signature;
    public BillingRequest billingDetails;
    public String subscriptionId;// Your existing BillingRequest object

}
