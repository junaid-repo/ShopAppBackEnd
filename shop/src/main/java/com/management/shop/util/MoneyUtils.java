package com.management.shop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int AMOUNT_SCALE = 2;
    public static final int PERCENTAGE_SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    public static BigDecimal decimal(Number value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(value.toString());
    }

    public static BigDecimal amount(Number value) {
        return decimal(value).setScale(AMOUNT_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal percentage(Number value) {
        return decimal(value).setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
    }

    public static double asAmountDouble(Number value) {
        return amount(value).doubleValue();
    }

    public static double asPercentageDouble(Number value) {
        return percentage(value).doubleValue();
    }
}
