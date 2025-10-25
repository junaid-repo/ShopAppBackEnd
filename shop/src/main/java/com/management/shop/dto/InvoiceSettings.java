package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceSettings {
    private Boolean addDueDate;
    private Boolean combineAddresses;
    private Boolean showPaymentStatus;
    private Boolean removeTerms;
    private Boolean showCustomerGstin;

    Boolean showTotalDiscountPercentage ;
    Boolean showIndividualDiscountPercentage ;
    Boolean showShopPanOnInvoice ;
    Boolean showSupportInfoOnInvoice ;
    Boolean showRateColumn ;
    Boolean showHsnColumn ;
}
