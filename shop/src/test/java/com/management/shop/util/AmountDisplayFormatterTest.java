package com.management.shop.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountDisplayFormatterTest {

    private final AmountDisplayFormatter formatter = AmountDisplayFormatter.INSTANCE;

    @Test
    void displaysZeroWithoutDecimalPlaces() {
        assertEquals("0", formatter.grouped(BigDecimal.ZERO));
        assertEquals("0", formatter.plain(new BigDecimal("0.004")));
    }

    @Test
    void displaysNonZeroValuesWithTwoDecimalPlaces() {
        assertEquals("1,234.50", formatter.grouped(new BigDecimal("1234.5")));
        assertEquals("10.01", formatter.plain(new BigDecimal("10.005")));
    }

    @Test
    void displaysRoundedIntegersWhenDecimalPlacesAreDisabled() {
        AmountDisplayFormatter integerFormatter = AmountDisplayFormatter.forSetting(false);

        assertEquals("1,235", integerFormatter.grouped(new BigDecimal("1234.50")));
        assertEquals("10", integerFormatter.plain(new BigDecimal("10.49")));
        assertEquals("0", integerFormatter.plain(new BigDecimal("0.49")));
    }

    @Test
    void keepsTaxPercentagesAccurateInIntegerAmountMode() {
        AmountDisplayFormatter integerFormatter = AmountDisplayFormatter.forSetting(false);

        assertEquals("2.50", integerFormatter.percentage(new BigDecimal("2.5")));
        assertEquals("0", integerFormatter.percentage(BigDecimal.ZERO));
    }
}
