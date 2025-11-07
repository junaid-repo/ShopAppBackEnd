package com.management.shop.dto;

public interface GstByStateDto {
    String getState();
    Double getTotalTaxableValue();
    Double getTotalCgst();
    Double getTotalSgst();
    Double getTotalIgst();
    Double getTotalGst();
}
