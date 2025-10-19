package com.management.shop.dto;

import lombok.Data;

@Data
public class ShopBasicDetailsRequest {
    private String shopName;
    private String shopSlogan;
    private String shopAddress;
    private String shopEmail;
    private String shopPhone;
    private String shopPincode;
    private String shopCity;
    private String shopState;
    private String gstin;
    private String panNumber;
}
