package com.management.shop.controller;

import com.management.shop.dto.*;
import com.management.shop.service.ShopService;
import com.management.shop.service.SubscribtionsService;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class SubsController {

    @Autowired
    SubscribtionsService serv;

    @Autowired
    ShopService shopService;

    @Value("${razorpay.key.secret}")
    private String keySecret;
    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${internal.api.key}")
    private String internalApiKey;

    @PostMapping("api/shop/subscription/create")
    ResponseEntity<Map<String, Object>> addSubscription(@RequestBody SubsriptionRequest request){
        Map<String, Object> response=    serv.saveSubscription(request);

        return ResponseEntity.ok(response);

    }

    @PostMapping("/internal/create/subscription")
    ResponseEntity<Map<String, Object>> addInternalSubscription(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @Valid @RequestBody InternalSubscriptionRequest request) {

        if (!isValidInternalApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid service credentials"));
        }

        SubsriptionRequest subscriptionRequest = new SubsriptionRequest();
        subscriptionRequest.setPlanType(request.getPlanType());
        subscriptionRequest.setAmount(request.getAmount());
        subscriptionRequest.setType(request.getType());

        return ResponseEntity.ok(
                serv.provisionInitialSubscription(subscriptionRequest, request.getUsername()));
    }

    @PostMapping("/internal/create/data")
    ResponseEntity<Map<String, Object>> addDummyProductsAndCustomers(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @Valid @RequestBody DummyDataDto request) {

        if (!isValidInternalApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid service credentials"));
        }
        List<ProductRequest> dummyProducts = request.getDummyProducts();

        List<CustomerRequest> dummyCustomers = request.getDummyCustomers();

        try {
            dummyProducts.stream().forEach(i -> {
                ;
                shopService.saveProductInternal(i);
            });
            dummyCustomers.stream().forEach(i -> {
                shopService.saveCustomerInternal(i);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


        return ResponseEntity.ok(null);
    }

    private boolean isValidInternalApiKey(String apiKey) {
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                internalApiKey.getBytes(StandardCharsets.UTF_8));
    }
    @PostMapping("api/shop/subscription/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyAndBillRequest request) {
        System.out.println("Inside the verifyPayment method for card payment "+request.getSubscriptionId());

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.razorpay_order_id);
            options.put("razorpay_payment_id", request.razorpay_payment_id);
            options.put("razorpay_signature", request.razorpay_signature);

            boolean isValid = Utils.verifyPaymentSignature(options, this.keySecret);

            if (isValid) {
                // 3. If signature is valid, call your billing service!
                System.out.println("Payment verified. Proceeding to save the bill." + request.getRazorpay_payment_id());
                System.out.println("Payment verified. Proceeding to save the bill." + request.getRazorpay_order_id());
                System.out.println("Payment verified.  Proceeding to save the bill." + request.getRazorpay_signature());

                // This replaces the direct call to /api/shop/do/billing
                String billingResponse = serv.updateSubsription(request);



                // Return the response from your billing service
                return ResponseEntity.ok(billingResponse);
            } else {
                return ResponseEntity.status(400).body("Invalid payment signature.");
            }
        } catch (Exception e) {
            // Your service's doPayment method might throw an exception
            return ResponseEntity.status(500).body("Error during billing: " + e.getMessage());
        }
    }

    @GetMapping("api/shop/subscription/details")
    ResponseEntity<List<Map<String, Object>>> getSubscriptionDetails(){
        List<Map<String, Object>> response=    serv.getSubscriptionDetails();

        return ResponseEntity.ok(response);

    }

    @GetMapping("api/shop/subscription/testDetails")
    ResponseEntity<String> testScheulder(){
        String response=    serv.testScheduler();

        return ResponseEntity.ok(response);

    }

    @PostMapping("api/shop/subscription/save-billing-details")
    public ResponseEntity<Map<String, String>> saveSubscriptionBillingDetails(@RequestBody Map<String, String> request) {

        System.out.println("The request payload for saveSubscriptionBillingDetails  is-->" + request);

        Map<String, String> response = serv.saveSubscriptionBillingDetails(request);

        if(response.get("status").equals("failure")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }





}
