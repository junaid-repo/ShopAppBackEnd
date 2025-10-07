package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.management.shop.entity.PaymentEntity;

public interface SalesPaymentRepository extends JpaRepository<PaymentEntity, Integer> {

	@Query(value = "select * from billing_payments bp where bp.billing_id=?1  and user_id = ?2", nativeQuery = true)
	PaymentEntity findPaymentDetails(Integer id, String userId);

	@Query(value = "SELECT DATE_FORMAT(bp.created_date, '%b') AS month, " + "COUNT(bp.id) AS paymentCount "
			+ "FROM billing_payments bp " + "WHERE bp.payment_method IN ('CARD', 'UPI') "
			+ "AND bp.created_date BETWEEN :fromDate AND :toDate and user_id=:userId "
			+ "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') "
			+ "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
	List<Object[]> getMonthlyPaymentCounts(@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate, @Param("userId") String userId);

    @Modifying
    @Transactional
    @Query(value = "update billing_payments bp, billing_details bd  set bp.payment_reference_number=?1 where bp.billing_id = bd.id and bp.user_id=?3 and bd.invoice_number =?2", nativeQuery = true)
    void updatePaymentReferenceNumber(String paymentRef, String orderRef, String s);


    @Query("""
        SELECT bp.paymentMethod AS paymentMethod, COUNT(bp) AS count
        FROM PaymentEntity bp
        WHERE bp.userId = :userId
          AND bp.createdDate BETWEEN :startDate AND :endDate
        GROUP BY bp.paymentMethod
    """)
    List<Map<String, Object>> getPaymentBreakdown(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT *   "
            + "FROM billing_payments bp " + "WHERE   "
            + " bp.created_date BETWEEN :fromDate AND :toDate and user_id=:userId "
            + "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
    List<PaymentEntity> getPaymentList(@Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate, @Param("userId") String userId);
}
