package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportSchedulerSettings {

    private Boolean enabled;
    private String email;
    private String reportTypes;
}
