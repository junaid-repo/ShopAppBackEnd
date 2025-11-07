package com.management.shop.dto;

public interface MonthlyGstSummaryDto {
    String getMonthYear();
    Double getTotalTaxableValue();
    Double getTotalCgst();
    Double getTotalSgst();
    Double getTotalIgst();
    Double getTotalGst();
}
