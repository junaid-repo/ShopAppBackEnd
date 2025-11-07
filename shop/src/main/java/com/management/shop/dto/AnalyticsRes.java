package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AnalyticsRes {
    List<PieAnalyticsMap> paymentStatus;
    List<PieAnalyticsMap> customerGst;
    List<PieAnalyticsMap> invoiceStatus;
    List<PieAnalyticsMap> monthlyProfits;
    Double totalProfit;
    List<PieAnalyticsMap> salesAndRevenue;
    List<PieAnalyticsMap> monthlyStockSold;
    Double totalRevenue;
    Double totalStockSold;
    List<PieAnalyticsMap> topProducts;
    List<PieAnalyticsMap> monthlySales;
    Double totalSales;
}
