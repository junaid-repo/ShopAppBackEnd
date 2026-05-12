package com.management.shop.util;

import com.management.shop.entity.ProductCategory;
import com.management.shop.entity.ProductEntity;
import com.management.shop.repository.ProductCategoryRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CSVUtil {

    @Autowired
    private ProductCategoryRepository productCategoryRepo;

    public String extractUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public byte[] exportAllProductAsCSV(List<ProductEntity> productList) {

        // --- UPDATED: Removed selectedProductId ---
        final String[] HEADERS = {
                "name", "hsn", "category",
                "costPrice", "price", "stock", "tax", "location"
        };

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(outputStream);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(HEADERS));

            productList.stream().forEach(obj -> {
                try {
                    // --- UPDATED: Removed obj.getId() ---
                    printer.printRecord(
                            obj.getName(),
                            obj.getHsn(),
                            obj.getCategory(),
                            obj.getCostPrice(),
                            obj.getPrice(),
                            obj.getStock(),
                            obj.getTaxPercent(),
                            obj.getLocation());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            printer.flush();

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void addCategories(String unquote, int lineNumber) {
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
    }
}