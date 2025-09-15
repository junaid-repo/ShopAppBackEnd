package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.management.shop.dto.MonthlyAnalyticDTO;
import com.management.shop.entity.BillingEntity;

public interface BillingRepository extends JpaRepository<BillingEntity, Integer> {

	@Query(value = "select * from billing_details where invoice_number=?1  and user_id = ?2", nativeQuery = true)
	BillingEntity findOrderByReference(String orderReferenceNumber, String userId);

	@Query(value = "select * from billing_details where created_date BETWEEN ?1 AND ?2 and user_id=?3", nativeQuery = true)
	List<BillingEntity> findPaymentsByDateRange(LocalDateTime fromDate, LocalDateTime toDate, String userId);

	@Query(value = "select * from billing_details where created_date>=?1  and user_id = ?2", nativeQuery = true)
	List<BillingEntity> findAllByDayRange(LocalDateTime localDateTime, String userId);

	@Query(value = "SELECT * FROM billing_details WHERE created_date >= ?1 	   AND created_date < ?2 and user_id=?3", nativeQuery = true)
	List<BillingEntity> findAllCreatedToday(LocalDateTime startOfDay, LocalDateTime endOfDay, String userId);

    @Query(value = "SELECT DATE_FORMAT(bp.created_date, '%b') AS month, " +
            "SUM(bp.total) AS count, " +
            "SUM(bd.total_profit_oncp) AS totalProfit " +
            "FROM billing_payments bp " +
            "JOIN billing_details bd ON bp.billing_id = bd.id " +
            "WHERE bp.created_date BETWEEN :fromDate AND :toDate " +
            "AND bp.user_id = :userId " +
            "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') " +
            "ORDER BY MONTH(bp.created_date)",
            nativeQuery = true)
    List<Object[]> getMonthlySalesSummary(@Param("fromDate") LocalDateTime fromDate,
                                          @Param("toDate") LocalDateTime toDate,
                                          @Param("userId") String userId);

    @Query(value = "SELECT DATE_FORMAT(bp.created_date, '%b') AS month, " +
            "SUM(ps.quantity) AS totalStocksSold " +
            "FROM billing_payments bp " +
            "JOIN product_sales ps ON bp.id = ps.billing_id " +
            "WHERE bp.created_date BETWEEN :fromDate AND :toDate " +
            "AND bp.user_id = :userId " +
               "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') " +
                       "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
    List<Object[]> getMonthlyStocksSold(@Param("fromDate") LocalDateTime fromDate,
                                        @Param("toDate") LocalDateTime toDate,
                                        @Param("userId") String userId);


	@Query(value = "SELECT DATE_FORMAT(created_date, '%b') AS month, " + "SUM(tax) AS count " + "FROM billing_payments "
			+ "WHERE created_date BETWEEN :fromDate AND :toDate and user_id=:userId "
			+ "GROUP BY MONTH(created_date), DATE_FORMAT(created_date, '%b') "
			+ "ORDER BY MONTH(created_date)", nativeQuery = true)
	List<Object[]> getMonthlyTaxesSummary(@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate, @Param("userId") String userId);

    @Query(value = "select * from billing_details where user_id=?1 and created_date BETWEEN ?2 AND ?3", nativeQuery = true)
    List<BillingEntity> findAllWithUserId(String userId, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "select * from billing_details where user_id=?1 order by created_date desc limit ?2", nativeQuery = true)
    List<BillingEntity> findNNumberWithUserId(String userId, int count);

    @Query(value = "select * from billing_details where user_id=?1", nativeQuery = true)
    Page<BillingEntity> findAllByUserId(String userId, Pageable pageable);

    @Query(
            value = "SELECT b.* FROM billing_details b " +
                    "JOIN shop_customer s ON b.customer_id = s.id " +
                    "WHERE b.user_id = :userId " +
                    "AND (" +
                    "  LOWER(b.invoice_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                    "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                    "  OR CAST(b.total_amount AS CHAR) LIKE CONCAT('%', :searchTerm, '%')" +
                    ")",
            countQuery = "SELECT COUNT(*) FROM billing_details b " +
                    "JOIN shop_customer s ON b.customer_id = s.id " +
                    "WHERE b.user_id = :userId " +
                    "AND (" +
                    "  LOWER(b.invoice_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                    "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                    "  OR CAST(b.total_amount AS CHAR) LIKE CONCAT('%', :searchTerm, '%')" +
                    ")",
            nativeQuery = true
    )
    Page<BillingEntity> findByUserIdAndSearchNative(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );



}
