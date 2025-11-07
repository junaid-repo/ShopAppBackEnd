package com.management.shop.dto;

public interface GstByHsnDto {
    String getHsn();
    String getProductName();
    Integer getTotalQuantity();
    Double getTotalTaxableValue();
    Double getTotalCgst();
    Double getTotalSgst();
    Double getTotalIgst();
    Double getTotalGst();
}
