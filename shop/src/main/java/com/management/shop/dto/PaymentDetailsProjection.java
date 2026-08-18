package com.management.shop.dto;

import java.time.LocalDateTime;

/**
 * Lightweight view used by the joined payment-list query.
 */
public interface PaymentDetailsProjection {

    String getId();

    String getSaleId();

    LocalDateTime getPaymentDate();

    Double getAmount();

    String getMethod();

    Double getPaid();

    Double getDue();

    String getStatus();

    Integer getReminderCount();

    String getCustomerName();
}
