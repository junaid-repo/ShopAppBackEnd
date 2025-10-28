package com.management.shop.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.management.shop.dto.ProductRequest;

@Service
public class CSVUpload {


    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "selectedProductId", "name", "hsn", "category", "costPrice", "price", "stock", "tax"
    );

    // --- NEW: Constants for Validation ---
    private static final List<String> VALID_CATEGORIES = Arrays.asList("Product", "Services", "Others");
    private static final List<Integer> VALID_TAX_SLABS = Arrays.asList(0, 5, 12, 18, 28);

    // Regex splits on commas that are not inside quotes: a simple, practical CSV splitter
    private static final Pattern CSV_SPLIT = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    public List<ProductRequest> parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded.");
        }

        List<ProductRequest> products = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) {
                return products; // empty file
            }

            validateHeader(header);

            String line;
            int lineNumber = 1; // already read header
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] tokens = CSV_SPLIT.split(line, -1);
                if (tokens.length != 8) {
                    // --- FIX: Corrected error message from 6 to 8 ---
                    throw new IllegalArgumentException("Invalid column count at line " + lineNumber + " (expected 8)");
                }

                String selectedProductIdStr = unquote(tokens[0]);
                String name = unquote(tokens[1]);

                // --- VALIDATION 1: HSN (must be a number) ---
                String hsn = validateHsn(unquote(tokens[2]), lineNumber);

                // --- VALIDATION 2: Category ---
                String category = validateCategory(unquote(tokens[3]), lineNumber);

                Integer costPrice = parseInt(unquote(tokens[4]), "costPrice", lineNumber);
                Integer price = parseInt(unquote(tokens[5]), "price", lineNumber);

                // --- VALIDATION 3: CostPrice vs SellingPrice ---
                if (costPrice > price) {
                    throw new IllegalArgumentException("Validation error at line " + lineNumber +
                            ": Cost Price (" + costPrice + ") cannot be more than Selling Price (" + price + ")");
                }

                Integer stock = parseInt(unquote(tokens[6]), "stock", lineNumber);
                Integer taxRaw = parseInt(unquote(tokens[7]), "tax", lineNumber);

                // --- VALIDATION 4: Tax Percent ---
                Integer tax = validateTax(taxRaw, lineNumber);


                products.add(ProductRequest.builder()
                        .selectedProductId(parseInt(selectedProductIdStr, "selectedProductId", lineNumber)) // Also parse ID
                        .name(name)
                        .hsn(hsn)
                        .costPrice(costPrice)
                        .price(price)
                        .category(category)
                        .stock(stock)
                        .tax(tax)
                        .build());
            }
        }

        return products;
    }

    private static void validateHeader(String headerLine) {
        String[] headers = CSV_SPLIT.split(headerLine, -1);
        if (headers.length != EXPECTED_HEADERS.size()) {
            // --- FIX: Corrected error message from 6 to 8 ---
            throw new IllegalArgumentException("CSV header must have " + EXPECTED_HEADERS.size() + " columns: " + EXPECTED_HEADERS);
        }
        for (int i = 0; i < headers.length; i++) {
            String actual = unquote(headers[i]).trim();
            String expected = EXPECTED_HEADERS.get(i);
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException("CSV header mismatch at column " + (i + 1) +
                        ": expected '" + expected + "' but found '" + actual + "'");
            }
        }
    }

    private static String unquote(String s) {
        // --- FIX: Removed incorrect validation logic from this helper method ---
        if (s == null) return null;
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        // Unescape doubled quotes inside quoted fields
        return s.replace("\"\"", "\"");
    }

    private static double parseDouble(String value, String field, int line) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + field + " at line " + line + ": '" + value + "'");
        }
    }

    private static int parseInt(String value, String field, int line) {
        try {
            // Allow empty strings for nullable integer fields
            if (value == null || value.trim().isEmpty()) {
                // You might want to return 0 or null depending on your DTO/Entity
                // Returning 0 for simplicity as per original code.
                return 0;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + field + " at line " + line + ": must be a whole number, but found '" + value + "'");
        }
    }

    // --- NEW HELPER METHODS FOR VALIDATION ---

    /**
     * Validates that the HSN string is a number.
     */
    private static String validateHsn(String hsn, int line) {
        if (hsn == null || hsn.trim().isEmpty()) {
            return hsn; // Allow empty HSN
        }
        try {
            Long.parseLong(hsn.trim()); // Try parsing as Long to check if it's numeric
            return hsn.trim();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid HSN at line " + line + ": must be a number, but found '" + hsn + "'");
        }
    }

    /**
     * Validates that the category is one of the allowed values.
     */
    private static String validateCategory(String category, int line) {
        if (category == null || !VALID_CATEGORIES.contains(category.trim())) {
            throw new IllegalArgumentException("Invalid Category at line " + line + ": must be one of " + VALID_CATEGORIES + ", but found '" + category + "'");
        }
        return category.trim();
    }

    /**
     * Validates that the tax is one of the allowed slab values.
     */
    private static int validateTax(int tax, int line) {
        if (!VALID_TAX_SLABS.contains(tax)) {
            throw new IllegalArgumentException("Invalid Tax Percent at line " + line + ": must be one of " + VALID_TAX_SLABS + ", but found '" + tax + "'");
        }
        return tax;
    }
}
