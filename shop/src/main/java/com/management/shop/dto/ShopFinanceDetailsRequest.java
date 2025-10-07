package com.management.shop.dto;

import lombok.Data;

@Data
public class ShopFinanceDetailsRequest {

    private String gstin;
    private String pan;
    private String upi;
    private String bankHolder;
    private String bankAccount;
    private String bankIfsc;
    private String bankName;
    private String bankAddress;
}
