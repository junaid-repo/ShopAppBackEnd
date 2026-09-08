package com.management.shop.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves every Dashboard range token to one consistent, half-open date range.
 * Financial years follow the Indian April-to-March calendar.
 */
public final class DashboardDateRange {

    private static final Pattern FINANCIAL_YEAR_PATTERN = Pattern.compile("^fy-(\\d{4})-(?:\\d{2}|\\d{4})$");

    private DashboardDateRange() {
    }

    public static Range resolve(String selection) {
        LocalDate today = LocalDate.now();
        String normalizedSelection = selection == null ? "today" : selection.trim();

        Matcher financialYearMatcher = FINANCIAL_YEAR_PATTERN.matcher(normalizedSelection);
        if (financialYearMatcher.matches()) {
            int startYear = Integer.parseInt(financialYearMatcher.group(1));
            LocalDate start = LocalDate.of(startYear, 4, 1);
            return new Range(start.atStartOfDay(), start.plusYears(1).atStartOfDay(), false, true);
        }

        LocalDate start = switch (normalizedSelection) {
            case "today" -> today;
            case "lastWeek" -> today.minusDays(6);
            case "lastMonth" -> today.minusDays(29);
            case "lastYear" -> today.minusDays(364);
            default -> throw new IllegalArgumentException("Unsupported dashboard range: " + normalizedSelection);
        };

        return new Range(start.atStartOfDay(), today.plusDays(1).atStartOfDay(),
                "today".equals(normalizedSelection), false);
    }

    public record Range(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            boolean hourly,
            boolean financialYear
    ) {
        public LocalDateTime endInclusive() {
            return endExclusive.minusNanos(1);
        }
    }
}
