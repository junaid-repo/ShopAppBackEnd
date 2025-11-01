package com.management.shop.repository;

import com.management.shop.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Integer> {

    @Query(value = "SELECT * FROM billing_payments_history WHERE order_number = ?1 AND user_id = ?2", nativeQuery = true)
    List<PaymentHistory> findPaymentHistoryByOrderNumber(String orderNo, String userId);
}
