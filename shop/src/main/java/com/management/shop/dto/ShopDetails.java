package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopDetails {


    private String gstin;
    private String pan;
    private String upi;
    private String bankHolder;
    private String bankAccount;
    private String bankIfsc;
    private String bankName;
    private String bankAddress;
    private String terms1;
    private String terms2;
    private String terms3;


}
