package com.management.shop.gobalusers.client;

import com.management.shop.gobalusers.dto.InternalDummyDataRequest;
import com.management.shop.gobalusers.dto.InternalSubscriptionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "shopSubscriptionClient", url = "${shop.service.url}")
public interface SubscriptionClient {

    @PostMapping("/internal/create/subscription")
    Map<String, Object> createSubscription(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestBody InternalSubscriptionRequest request);

    @PostMapping("/internal/create/data")
    Map<String, Object> createDummyData(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestBody InternalDummyDataRequest request);
}

