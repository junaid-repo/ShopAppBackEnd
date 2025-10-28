package com.management.shop.util;

import com.management.shop.entity.ProductEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Component
public class CSVUtil {

    public byte[] exportAllProductAsCSV(List<ProductEntity> productList) {

        final String[] HEADERS = {
                "selectedProductId", "name", "hsn", "category",
                "costPrice", "price", "stock", "tax"
        };


        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(outputStream);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(HEADERS));

            productList.stream().forEach(obj->{
                try {
                    printer.printRecord(obj.getId(),
                            obj.getName(),
                            obj.getHsn(),
                            obj.getCategory(),
                            obj.getCostPrice(),
                            obj.getPrice(),
                            obj.getStock(),
                            obj.getTaxPercent());
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
}
