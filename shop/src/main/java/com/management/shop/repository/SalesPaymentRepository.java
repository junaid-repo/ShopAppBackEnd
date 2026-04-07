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

    // Kept untouched so frontend can still view payment details of cancelled orders
    @Query(value = "select * from billing_payments bp where bp.billing_id=?1 and user_id = ?2", nativeQuery = true)
    PaymentEntity findPaymentDetails(Integer id, String userId);

    @Query(value = "SELECT DATE_FORMAT(bp.created_date, '%b') AS month, COUNT(bp.id) AS paymentCount "
            + "FROM billing_payments bp "
            + "JOIN billing_details bd ON bp.billing_id = bd.id "
            + "WHERE bp.payment_method IN ('CARD', 'UPI', 'CASH') "
            + "AND bp.created_date BETWEEN :fromDate AND :toDate and bp.user_id=:userId AND bd.invoice_status = 'ACTIVE' "
            + "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') "
            + "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
    List<Object[]> getMonthlyPaymentCounts(@Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate, @Param("userId") String userId);

    @Modifying
    @Transactional
    @Query(value = "update billing_payments bp, billing_details bd set bp.payment_reference_number=?1 where bp.billing_id = bd.id and bp.user_id=?3 and bd.invoice_number =?2 AND bd.invoice_status = 'ACTIVE'", nativeQuery = true)
    void updatePaymentReferenceNumber(String paymentRef, String orderRef, String s);

    // Converted to Native Query to safely apply the JOIN
    @Query(value = """
        SELECT bp.payment_method AS paymentMethod, COUNT(bp.id) AS count
        FROM billing_payments bp
        JOIN billing_details bd ON bp.billing_id = bd.id
        WHERE bp.user_id = :userId AND bd.invoice_status = 'ACTIVE'
          AND bp.created_date BETWEEN :startDate AND :endDate
        GROUP BY bp.payment_method
    """, nativeQuery = true)
    List<Map<String, Object>> getPaymentBreakdown(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Converted to Native Query to safely apply the JOIN
    @Query(value = """
        SELECT 
               COALESCE(SUM(bp.paid), 0) AS totalPaid,
               COALESCE(SUM(bp.to_be_paid), 0) AS totalDue
        FROM billing_payments bp
        JOIN billing_details bd ON bp.billing_id = bd.id
        WHERE bp.user_id = :userId AND bd.invoice_status = 'ACTIVE'
          AND bp.created_date between :startDate and :endDate
    """, nativeQuery = true)
    List<Map<String, Object>> getPaymentStatusBreakdown(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT bp.* FROM billing_payments bp "
            + "JOIN billing_details bd ON bp.billing_id = bd.id "
            + "WHERE bp.created_date BETWEEN :fromDate AND :toDate and bp.user_id=:userId AND bd.invoice_status = 'ACTIVE' "
            + "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
    List<PaymentEntity> getPaymentList(@Param("fromDate") LocalDateTime fromDate,
                                       @Param("toDate") LocalDateTime toDate, @Param("userId") String userId);

    // Uses IN subquery for safe updates
    @Transactional
    @Modifying
    @Query(value="update billing_payments set reminder_count= reminder_count+1, updated_by=?2, updated_date=?3 where order_number=?1 and user_id=?2 AND billing_id IN (SELECT id FROM billing_details WHERE invoice_status = 'ACTIVE')", nativeQuery = true)
    void updateReminderCount(String orderNo, String username, LocalDateTime updatedDate);

    // Uses IN subquery for safe updates
    @Transactional
    @Modifying
    @Query(value = "UPDATE billing_payments SET paid = paid + ?4, to_be_paid = to_be_paid - ?4, updated_by = ?2, updated_date = ?3 WHERE order_number = ?1 AND user_id = ?2 AND billing_id IN (SELECT id FROM billing_details WHERE invoice_status = 'ACTIVE')", nativeQuery = true)
    void updateDueAmount(String orderNo, String username, LocalDateTime updatedDate, Double payingAmount);

    // Uses IN subquery for safe updates
    @Transactional
    @Modifying
    @Query(value = "UPDATE billing_payments SET status = ?3 WHERE order_number = ?1 AND user_id = ?2 AND billing_id IN (SELECT id FROM billing_details WHERE invoice_status = 'ACTIVE')", nativeQuery = true)
    void updatePaymentStatus(String orderNo, String username, String status);

    // Kept untouched
    @Query("select p from PaymentEntity p where p.orderNumber = ?1 and p.userId = ?2")
    PaymentEntity findByOrderNumber(String orderNo, String username);

    @Query(value="select bp.* from billing_payments bp JOIN billing_details bd ON bp.billing_id = bd.id where bp.to_be_paid>0 and bp.user_id=?1 AND bp.created_date < (NOW() - INTERVAL '24' HOUR) AND bd.invoice_status = 'ACTIVE'", nativeQuery = true)
    List<PaymentEntity> findByUserId(String username);

    // Converted to Native Query to safely apply the JOIN
    @Query(value = "SELECT bp.status, SUM(bp.total), COUNT(bp.id) " +
            "FROM billing_payments bp " +
            "JOIN billing_details bd ON bp.billing_id = bd.id " +
            "WHERE bp.user_id = :userId AND bd.invoice_status = 'ACTIVE' " +
            "AND bp.created_date BETWEEN :fromDate AND :toDate " +
            "GROUP BY bp.status", nativeQuery = true)
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
            "    AND p.user_id = ?3 AND b.invoice_status = 'ACTIVE' " +
            "ORDER BY " +
            "    p.created_date DESC", nativeQuery = true)
    List<PaymentReportDto> findPaymentReportByDateRange(LocalDateTime fromDate, LocalDateTime toDate, String userId);

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
            "    AND p.user_id = ?3 AND b.invoice_status = 'ACTIVE' " +
            "GROUP BY " +
            "    p.payment_method " +
            "ORDER BY " +
            "    totalAmount DESC", nativeQuery = true)
    List<PaymentSummaryDto> findPaymentSummaryByMethod(LocalDateTime fromDate, LocalDateTime toDate, String userId);

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
            "    AND p.user_id = ?3 AND b.invoice_status = 'ACTIVE' " +
            "GROUP BY " +
            "    p.status " +
            "ORDER BY " +
            "    totalAmount DESC", nativeQuery = true)
    List<PaymentSummaryDto> findPaymentSummaryByStatus(LocalDateTime fromDate, LocalDateTime toDate, String userId);

    @Query(value = "SELECT COALESCE(bp.payment_method, 'Unspecified') AS method_name, " +
            "SUM(bp.paid) AS total_amount " +
            "FROM billing_payments bp " +
            "JOIN billing_details bd ON bp.billing_id = bd.id " +
            "WHERE bp.user_id = :userId AND bd.invoice_status = 'ACTIVE' " +
            "AND bp.created_date >= :startDate AND bp.created_date <= :endDate " +
            "GROUP BY bp.payment_method " +
            "ORDER BY total_amount DESC",
            nativeQuery = true)
    List<Object[]> getPaymentMethodSummary(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           @Param("userId") String userId);
}