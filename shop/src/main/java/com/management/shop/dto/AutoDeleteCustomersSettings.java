package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AutoDeleteCustomersSettings {
    private boolean enabled;
    private int minSpent;
    private int inactiveDays;
}
