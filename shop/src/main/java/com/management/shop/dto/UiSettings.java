package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UiSettings {
    private boolean darkModeDefault;
    private boolean billingPageDefault;
    private boolean autoPrintInvoice;
    private boolean autoSendInvoice;

}
