package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopSettings implements Serializable {

    private UiSettings ui;
    private SchedulerSettings schedulers;
    private BillingSettings billing;
    private InvoiceSettings invoice;
    private ReportSchedulerSettings reports;
}
