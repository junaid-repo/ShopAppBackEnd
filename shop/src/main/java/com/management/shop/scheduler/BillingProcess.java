package com.management.shop.scheduler;


import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.BillingGstEntity;
import com.management.shop.entity.ProductSalesEntity;
import com.management.shop.repository.BillingGstRepository;
import com.management.shop.repository.BillingRepository;
import com.management.shop.repository.ProductSalesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class BillingProcess {

    @Autowired
    private BillingGstRepository gstRepo;

    @Autowired
    private BillingRepository billingRepo;

    @Autowired
    private ProductSalesRepository prodSalesRepo;


    public String saveGstListing(String orderId, String userId) {

        BillingEntity billingEntity = billingRepo.findOrderByReference(orderId, userId);
        List<ProductSalesEntity> prodSalesList = prodSalesRepo.findByOrderId(billingEntity.getId(), userId);

        // Step 1: Create a map to accumulate totals
        Map<String, Double> gstTotals = new HashMap<>();

        for (ProductSalesEntity prod : prodSalesList) {

            // CGST
            if (prod.getCgst() != null && prod.getCgst() > 0) {
                String key = "CGST@" + prod.getCgstPercentage();
                gstTotals.put(key, gstTotals.getOrDefault(key, 0.0) + prod.getCgst());
            }

            // SGST
            if (prod.getSgst() != null && prod.getSgst() > 0) {
                String key = "SGST@" + prod.getSgstPercentage();
                gstTotals.put(key, gstTotals.getOrDefault(key, 0.0) + prod.getSgst());
            }

            // IGST
            if (prod.getIgst() != null && prod.getIgst() > 0) {
                String key = "IGST@" + prod.getIgstPercentage();
                gstTotals.put(key, gstTotals.getOrDefault(key, 0.0) + prod.getIgst());
            }
        }

        // Step 2: Delete existing rows for this billing (optional, prevents duplicates)
        gstRepo.deleteByBillingIdAndUserId(billingEntity.getId(), userId);

        // Step 3: Save one grouped record per GST type + percentage
        for (Map.Entry<String, Double> entry : gstTotals.entrySet()) {
            String key = entry.getKey(); // Example: CGST@9
            Double totalAmount = entry.getValue();

            String[] parts = key.split("@");
            String gstType = parts[0];
            Double gstPercentage = Double.valueOf(parts[1]);

            BillingGstEntity gstListing = new BillingGstEntity();
            gstListing.setBillingId(billingEntity.getId());
            gstListing.setGstType(gstType);
            gstListing.setGstPercentage(gstPercentage);
            gstListing.setGstAmount(totalAmount.doubleValue());
            gstListing.setUpdatedDate(LocalDateTime.now());
            gstListing.setUpdatedBy(userId);
            gstListing.setUserId(userId);
            gstListing.setOrderNumber(orderId);

            gstRepo.save(gstListing);

        }


        return ("Gst Listing Saved");
    }


}
