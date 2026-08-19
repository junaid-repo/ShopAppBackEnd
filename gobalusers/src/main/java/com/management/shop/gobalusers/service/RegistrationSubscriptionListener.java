package com.management.shop.gobalusers.service;

import com.management.shop.gobalusers.client.SubscriptionClient;
import com.management.shop.gobalusers.dto.CustomerRequest;
import com.management.shop.gobalusers.dto.InternalDummyDataRequest;
import com.management.shop.gobalusers.dto.InternalSubscriptionRequest;
import com.management.shop.gobalusers.dto.ProductRequest;
import com.management.shop.gobalusers.event.UserRegistrationCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@Slf4j
public class RegistrationSubscriptionListener {

    private final SubscriptionClient subscriptionClient;
    private final String internalApiKey;
    private final String planType;
    private final String amount;
    private final String subscriptionType;

    public RegistrationSubscriptionListener(
            SubscriptionClient subscriptionClient,
            @Value("${internal.api.key}") String internalApiKey,
            @Value("${registration.subscription.plan-type:MONTHLY}") String planType,
            @Value("${registration.subscription.amount:0}") String amount,
            @Value("${registration.subscription.type:COMPLEMENTARY}") String subscriptionType) {
        this.subscriptionClient = subscriptionClient;
        this.internalApiKey = internalApiKey;
        this.planType = planType;
        this.amount = amount;
        this.subscriptionType = subscriptionType;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void provisionSubscription(UserRegistrationCompletedEvent event) {
        InternalSubscriptionRequest request = InternalSubscriptionRequest.builder()
                .username(event.username())
                .planType(planType)
                .amount(amount)
                .type(subscriptionType)
                .build();

        try {
            subscriptionClient.createSubscription(internalApiKey, request);
            log.info("Initial subscription provisioned for user {}", event.username());
        } catch (Exception exception) {
            log.error("Initial subscription provisioning failed for user {}", event.username(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void addDummyData(UserRegistrationCompletedEvent event) {

        List<CustomerRequest> customers = List.of(CustomerRequest.builder().username(event.username()).name("John Doe").email("na@na.com").phone("0000000000").gstNumber("na").city("na").customerState("Maharashtra").username(event.username()).build());
        List<ProductRequest> products=List.of(ProductRequest.builder().name("Sample Product").category("Product").price(10).stock(5).tax(18).costPrice(8).username(event.username()).build());
        var request = InternalDummyDataRequest.builder().dummyCustomers(customers).dummyProducts(products).build();

        try {
            subscriptionClient.createDummyData(internalApiKey, request);
            log.info("Initial subscription provisioned for user {}", event.username());
        } catch (Exception exception) {
            log.error("Initial subscription provisioning failed for user {}", event.username(), exception);
        }
    }
}
