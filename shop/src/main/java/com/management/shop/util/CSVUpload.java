package com.management.shop.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.management.shop.repository.ProductCategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.management.shop.dto.ProductRequest;

@Service
public class CSVUpload {

    @Autowired
    private ProductCategoryRepository productCategoryRepo;

    @Autowired
    private CSVUtil csvUtil;

    public String extractUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // --- UPDATED: Removed selectedProductId ---
    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "name", "hsn", "category", "costPrice", "price", "stock", "tax", "location"
    );

    private static final List<String> VALID_CATEGORIES = Arrays.asList("Product", "Services", "Others");
    private static final List<Integer> VALID_TAX_SLABS = Arrays.asList(0, 5, 12, 18, 28);

    private static final Pattern CSV_SPLIT = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    public List<ProductRequest> parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded.");
        }

        List<ProductRequest> products = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) {
                return products;
            }

            validateHeader(header);

            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] tokens = CSV_SPLIT.split(line, -1);

                // --- UPDATED: Expecting 8 columns now ---
                if (tokens.length != 8) {
                    throw new IllegalArgumentException("Invalid column count at line " + lineNumber + " (expected 8)");
                }

                String name = unquote(tokens[0]);
                String hsn = validateHsn(unquote(tokens[1]), lineNumber);
                String category = validateCategory(unquote(tokens[2]), lineNumber);
                Integer costPrice = parseInt(unquote(tokens[3]), "costPrice", lineNumber);
                Integer price = parseInt(unquote(tokens[4]), "price", lineNumber);

                if (costPrice > price) {
                    throw new IllegalArgumentException("Validation error at line " + lineNumber +
                            ": Cost Price (" + costPrice + ") cannot be more than Selling Price (" + price + ")");
                }

                Integer stock = parseInt(unquote(tokens[5]), "stock", lineNumber);
                Integer taxRaw = parseInt(unquote(tokens[6]), "tax", lineNumber);
                Integer tax = validateTax(taxRaw, lineNumber);
                String location = unquote(tokens[7]);

                products.add(ProductRequest.builder()
                        .name(name)
                        .hsn(hsn)
                        .costPrice(costPrice)
                        .price(price)
                        .category(category)
                        .stock(stock)
                        .tax(tax)
                        .location(location)
                        .build());
            }
        }
        return products;
    }

    @Transactional
    public List<ProductRequest> validateDataFromImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded.");
        }

        List<ProductRequest> products = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return products;

            validateHeader(header);

            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] tokens = CSV_SPLIT.split(line, -1);

                // --- UPDATED: Expecting 8 columns now ---
                if (tokens.length != 8) {
                    throw new IllegalArgumentException("Invalid column count at line " + lineNumber + " (expected 8)");
                }

                String name = unquote(tokens[0]);
                String hsn = validateHsn(unquote(tokens[1]), lineNumber);

                csvUtil.addCategories(unquote(tokens[2]), lineNumber);
                String category = unquote(tokens[2]);

                Integer costPrice = parseInt(unquote(tokens[3]), "costPrice", lineNumber);
                Integer price = parseInt(unquote(tokens[4]), "price", lineNumber);

                if (costPrice > price) {
                    throw new IllegalArgumentException("Validation error at line " + lineNumber +
                            ": Cost Price (" + costPrice + ") cannot be more than Selling Price (" + price + ")");
                }

                Integer stock = parseInt(unquote(tokens[5]), "stock", lineNumber);
                Integer taxRaw = parseInt(unquote(tokens[6]), "tax", lineNumber);
                Integer tax = validateTax(taxRaw, lineNumber);
                String location = unquote(tokens[7]);

                products.add(ProductRequest.builder()
                        .name(name)
                        .hsn(hsn)
                        .costPrice(costPrice)
                        .price(price)
                        .category(category)
                        .stock(stock)
                        .tax(tax)
                        .location(location)
                        .build());
            }
        }
        return products;
    }

    private static void validateHeader(String headerLine) {
        String[] headers = CSV_SPLIT.split(headerLine, -1);
        if (headers.length != EXPECTED_HEADERS.size()) {
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
        if (s == null) return null;
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\"\"", "\"");
    }

    private static int parseInt(String value, String field, int line) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + field + " at line " + line + ": must be a whole number, but found '" + value + "'");
        }
    }

    private static String validateHsn(String hsn, int line) {
        if (hsn == null || hsn.trim().isEmpty()) return hsn;
        try {
            Long.parseLong(hsn.trim());
            return hsn.trim();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid HSN at line " + line + ": must be a number, but found '" + hsn + "'");
        }
    }

    private static String validateCategory(String category, int line) {
        if (category == null || !VALID_CATEGORIES.contains(category.trim())) {
            throw new IllegalArgumentException("Invalid Category at line " + line + ": must be one of " + VALID_CATEGORIES + ", but found '" + category + "'");
        }
        return category.trim();
    }


    private static int validateTax(int tax, int line) {
        if (!VALID_TAX_SLABS.contains(tax)) {
            throw new IllegalArgumentException("Invalid Tax Percent at line " + line + ": must be one of " + VALID_TAX_SLABS + ", but found '" + tax + "'");
        }
        return tax;
    }
}
 /*   private void addCategories(String unquote, int lineNumber) {
        String categoryName = unquote.toLowerCase().replaceAll("\\s", "");
        List<ProductCategory> prodCatList = productCategoryRepo.getCategoryName(categoryName, extractUsername());

        if (prodCatList.isEmpty()) {
            try {
                var prodRepo = ProductCategory.builder()
                        .categoryName(unquote)
                        .type("product")
                        .updatedBy(extractUsername())
                        .username(extractUsername())
                        .updateDate(LocalDateTime.now())
                        .build();
                productCategoryRepo.save(prodRepo);
            } catch (Exception e) {
             }
        }
    }*/

