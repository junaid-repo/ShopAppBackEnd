package com.management.shop.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formats invoice amounts with optional decimal places; zero is always displayed as plain "0". */
public final class AmountDisplayFormatter {

    public static final AmountDisplayFormatter INSTANCE = new AmountDisplayFormatter(true);
    private static final AmountDisplayFormatter INTEGER_INSTANCE = new AmountDisplayFormatter(false);
    private final boolean decimalPlacesEnabled;

    private AmountDisplayFormatter(boolean decimalPlacesEnabled) {
        this.decimalPlacesEnabled = decimalPlacesEnabled;
    }

    public static AmountDisplayFormatter forSetting(Boolean enableDecimalPlace) {
        return Boolean.FALSE.equals(enableDecimalPlace) ? INTEGER_INSTANCE : INSTANCE;
    }

    public String grouped(Object value) {
        return format(value, true, decimalPlacesEnabled);
    }

    public String plain(Object value) {
        return format(value, false, decimalPlacesEnabled);
    }

    public String percentage(Object value) {
        return format(value, false, true);
    }

    private String format(Object value, boolean grouped, boolean showDecimalPlaces) {
        BigDecimal amount = parse(value);
        if (amount.signum() == 0) {
            return "0";
        }

        DecimalFormat formatter = new DecimalFormat(
                showDecimalPlaces
                        ? (grouped ? "#,##0.00" : "0.00")
                        : (grouped ? "#,##0" : "0"),
                DecimalFormatSymbols.getInstance(Locale.ROOT));
        formatter.setRoundingMode(MoneyUtils.ROUNDING_MODE);
        return formatter.format(amount);
    }

    private BigDecimal parse(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number number) {
            return MoneyUtils.amount(number);
        }
        try {
            return MoneyUtils.amount(new BigDecimal(String.valueOf(value)));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
