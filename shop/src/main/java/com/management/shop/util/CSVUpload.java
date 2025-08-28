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
	            "selectedProductId", "name", "category", "costPrice", "price", "stock", "tax"
	    );
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
	                if (tokens.length != 7) {
	                    throw new IllegalArgumentException("Invalid column count at line " + lineNumber + " (expected 6)");
	                }

	                String selectedProductId = unquote(tokens[0]);
	                String name = unquote(tokens[1]);
	                String category = unquote(tokens[2]);
                    Integer costPrice= parseInt(unquote(tokens[3]), "costPrice", lineNumber);
	                Integer price = parseInt(unquote(tokens[4]), "price", lineNumber);
	                Integer stock = parseInt(unquote(tokens[5]), "stock", lineNumber);
	                Integer tax = parseInt(unquote(tokens[6]), "tax", lineNumber);

	                products.add(ProductRequest.builder().selectedProductId(Integer.parseInt(selectedProductId))
	                		.name(name)
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
	            throw new IllegalArgumentException("CSV header must have 6 columns: " + EXPECTED_HEADERS);
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
	            return Integer.parseInt(value.trim());
	        } catch (Exception e) {
	            throw new IllegalArgumentException("Invalid " + field + " at line " + line + ": '" + value + "'");
	        }
	    }
}
