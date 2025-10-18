package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulerSettings {
    private boolean lowStockAlerts;
    private int autoDeleteNotificationsDays;
    private AutoDeleteCustomersSettings autoDeleteCustomers;
}
