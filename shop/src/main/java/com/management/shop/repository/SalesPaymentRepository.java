package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.management.shop.dto.PaymentReportDto;
import com.management.shop.dto.PaymentSummaryDto;
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
			+ "FROM billing_payments bp " + "WHERE bp.payment_method IN ('CARD', 'UPI', 'CASH') "
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


    @Transactional
    @Modifying
    @Query(value="update billing_payments set  reminder_count= reminder_count+1, updated_by=?2, updated_date=?3 where order_number=?1 and user_id=?2", nativeQuery = true)
    void updateReminderCount(String orderNo, String username, LocalDateTime updatedDate);

    @Transactional
    @Modifying
    @Query(value = "UPDATE billing_payments SET paid = paid + ?4, to_be_paid = to_be_paid - ?4, updated_by = ?2, updated_date = ?3 WHERE order_number = ?1 AND user_id = ?2", nativeQuery = true)
    void updateDueAmount(String orderNo, String username, LocalDateTime updatedDate, Double payingAmount);

    @Transactional
    @Modifying
    @Query(value = "UPDATE billing_payments SET status = ?3 WHERE order_number = ?1 AND user_id = ?2", nativeQuery = true)
    void updatePaymentStatus(String orderNo, String username, String status);

    PaymentEntity findByOrderNumber(String orderNo);

    @Query(value="select * from billing_payments bp where bp.to_be_paid>0 and bp.user_id=?1 AND bp.created_date < (NOW() - INTERVAL '24' HOUR)", nativeQuery = true)
    List<PaymentEntity> findByUserId(String username);



    @Query("SELECT b.status, SUM(b.total), COUNT(b) " +
            "FROM PaymentEntity b " +
            "WHERE b.userId = :userId " +
            "AND b.createdDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY b.status")
    List<Object[]> getCombinedPaymentSummary(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("userId") String userId
    );


    @Query(value = "SELECT " +
            "    p.payment_reference_number as paymentReferenceNumber, " +
            "    b.invoice_number as invoiceNumber, " +
            "    p.created_date as createdDate, " +
            "    p.payment_method as paymentMethod, " +
            "    p.total as total, " +
            "    p.paid as paid, " +
            "    p.to_be_paid as toBePaid, " +
            "    p.status as status " +
            "FROM " +
            "    billing_payments p " +
            "JOIN " +
            "    billing_details b ON p.billing_id = b.id AND p.user_id = b.user_id " +
            "WHERE " +
            "    p.created_date BETWEEN ?1 AND ?2 " +
            "    AND p.user_id = ?3 " +
            "ORDER BY " +
            "    p.created_date DESC", nativeQuery = true)
    List<PaymentReportDto> findPaymentReportByDateRange(LocalDateTime fromDate, LocalDateTime toDate, String userId);

    // --- NEW METHOD 2: For Summary by Method ---
    @Query(value = "SELECT " +
            "    p.payment_method as category, " +
            "    SUM(p.total) as totalAmount, " +
            "    GROUP_CONCAT(b.invoice_number SEPARATOR ', ') as invoiceList " +
            "FROM " +
            "    billing_payments p " +
            "JOIN " +
            "    billing_details b ON p.billing_id = b.id AND p.user_id = b.user_id " +
            "WHERE " +
            "    p.created_date BETWEEN ?1 AND ?2 " +
            "    AND p.user_id = ?3 " +
            "GROUP BY " +
            "    p.payment_method " +
            "ORDER BY " +
            "    totalAmount DESC", nativeQuery = true)
    List<PaymentSummaryDto> findPaymentSummaryByMethod(LocalDateTime fromDate, LocalDateTime toDate, String userId);

    // --- NEW METHOD 3: For Summary by Status ---
    @Query(value = "SELECT " +
            "    p.status as category, " +
            "    SUM(p.total) as totalAmount, " +
            "    GROUP_CONCAT(b.invoice_number SEPARATOR ', ') as invoiceList " +
            "FROM " +
            "    billing_payments p " +
            "JOIN " +
            "    billing_details b ON p.billing_id = b.id AND p.user_id = b.user_id " +
            "WHERE " +
            "    p.created_date BETWEEN ?1 AND ?2 " +
            "    AND p.user_id = ?3 " +
            "GROUP BY " +
            "    p.status " +
            "ORDER BY " +
            "    totalAmount DESC", nativeQuery = true)
    List<PaymentSummaryDto> findPaymentSummaryByStatus(LocalDateTime fromDate, LocalDateTime toDate, String userId);
}
