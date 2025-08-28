package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.management.shop.entity.PaymentEntity;

public interface SalesPaymentRepository extends JpaRepository<PaymentEntity, Integer> {

	@Query(value = "select * from billing_payments bp where bp.billing_id=?1", nativeQuery = true)
	PaymentEntity findPaymentDetails(Integer id);

	@Query(value = "SELECT DATE_FORMAT(bp.created_date, '%b') AS month, " + "COUNT(bp.id) AS paymentCount "
			+ "FROM billing_payments bp " + "WHERE bp.payment_method IN ('CARD', 'UPI') "
			+ "AND bp.created_date BETWEEN :fromDate AND :toDate "
			+ "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') "
			+ "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
	List<Object[]> getMonthlyPaymentCounts(@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

}
