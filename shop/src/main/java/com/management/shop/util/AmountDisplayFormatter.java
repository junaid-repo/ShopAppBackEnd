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

    /** Invoice line totals and grand totals are always rounded to the nearest whole amount. */
    public String roundedTotal(Object value) {
        return format(value, true, false);
    }

    private String format(Object value, boolean grouped, boolean showDecimalPlaces) {
        BigDecimal amount = parse(value);
        if (amount.signum() == 0) {
            return "0";
        }

        DecimalFormat formatter = new DecimalFormat(
                showDecimalPlaces ? "0.00" : "0",
                DecimalFormatSymbols.getInstance(Locale.ROOT));
        formatter.setRoundingMode(MoneyUtils.ROUNDING_MODE);
        String formatted = formatter.format(amount);
        return grouped ? applyIndianGrouping(formatted) : formatted;
    }

    private String applyIndianGrouping(String formatted) {
        boolean negative = formatted.startsWith("-");
        String unsigned = negative ? formatted.substring(1) : formatted;
        int decimalIndex = unsigned.indexOf('.');
        String integerPart = decimalIndex >= 0 ? unsigned.substring(0, decimalIndex) : unsigned;
        String decimalPart = decimalIndex >= 0 ? unsigned.substring(decimalIndex) : "";

        StringBuilder groupedInteger = new StringBuilder(integerPart);
        for (int separatorIndex = integerPart.length() - 3;
             separatorIndex > 0;
             separatorIndex -= 2) {
            groupedInteger.insert(separatorIndex, ',');
        }

        return (negative ? "-" : "") + groupedInteger + decimalPart;
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
