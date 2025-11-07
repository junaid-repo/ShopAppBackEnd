package com.management.shop.dto;

public interface GstByCustomerDto {
    String getName();
    String getPhone();
    String getGstNumber(); // From shop_customer table
    Double getTotalTaxableValue();
    Double getTotalCgst();
    Double getTotalSgst();
    Double getTotalIgst();
    Double getTotalGst();
}
