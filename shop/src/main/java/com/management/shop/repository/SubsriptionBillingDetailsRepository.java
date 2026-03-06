package com.management.shop.repository;

import com.management.shop.entity.SubscriptionBillingEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface SubsriptionBillingDetailsRepository extends JpaRepository<SubscriptionBillingEntity, Integer> {

    @Transactional
    @Modifying
    @Query(value = """
UPDATE subscription_billing_entity
SET subsription_payment_id = :razorpayOrderId,
    subscription_status = true,
    updated_date = :now
WHERE id = (
    SELECT id FROM (
        SELECT id
        FROM subscription_billing_entity
        WHERE username = :username
        ORDER BY created_date DESC
        LIMIT 1
    ) t
)
""", nativeQuery = true)
    void updateBillingDetailsByUsername(String username, String razorpayOrderId, LocalDateTime now);


    @Query(value = "SELECT * FROM subscription_billing_entity WHERE username = :s and subsription_payment_id is not null ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    SubscriptionBillingEntity findLatestByUsername(String s);
}
