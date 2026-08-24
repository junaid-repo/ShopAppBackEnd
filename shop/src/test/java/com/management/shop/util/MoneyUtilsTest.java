package com.management.shop.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyUtilsTest {

    @Test
    void roundsAmountsToTwoDecimalPlacesUsingHalfUp() {
        assertEquals(new BigDecimal("10.01"), MoneyUtils.amount(new BigDecimal("10.005")));
        assertEquals(new BigDecimal("10.00"), MoneyUtils.amount(new BigDecimal("10.004")));
    }

    @Test
    void keepsFractionalGstPercentages() {
        assertEquals(new BigDecimal("2.5000"), MoneyUtils.percentage(2.5));
    }

    @Test
    void normalizesFloatingPointInputBeforePersistence() {
        assertEquals(new BigDecimal("0.30"), MoneyUtils.amount(0.1d + 0.2d));
    }
}
