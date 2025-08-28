package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.management.shop.dto.MonthlyAnalyticDTO;
import com.management.shop.entity.BillingEntity;

public interface BillingRepository extends JpaRepository<BillingEntity, Integer> {

	@Query(value = "select * from billing_details where invoice_number=?1", nativeQuery = true)
	BillingEntity findOrderByReference(String orderReferenceNumber);

	@Query(value = "select * from billing_details where created_date BETWEEN ?1 AND ?2", nativeQuery = true)
	List<BillingEntity> findPaymentsByDateRange(LocalDateTime fromDate, LocalDateTime toDate);

	@Query(value = "select * from billing_details where created_date>=?1", nativeQuery = true)
	List<BillingEntity> findAllByDayRange(LocalDateTime localDateTime);

	@Query(value = "SELECT * FROM billing_details WHERE created_date >= ?1 	   AND created_date < ?2", nativeQuery = true)
	List<BillingEntity> findAllCreatedToday(LocalDateTime startOfDay, LocalDateTime endOfDay);

	@Query(value = "SELECT DATE_FORMAT(created_date, '%b') AS month, " + "SUM(total) AS count "
			+ "FROM billing_payments " + "WHERE created_date BETWEEN :fromDate AND :toDate "
			+ "GROUP BY MONTH(created_date), DATE_FORMAT(created_date, '%b') "
			+ "ORDER BY MONTH(created_date)", nativeQuery = true)
	List<Object[]> getMonthlySalesSummary(@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

	@Query(value = "SELECT DATE_FORMAT(bp.created_date, '%b') AS month, " + "SUM(ps.quantity) AS totalStocksSold "
			+ "FROM billing_payments bp " + "JOIN product_sales ps ON bp.id = ps.billing_id "
			+ "WHERE bp.created_date BETWEEN :fromDate AND :toDate "
			+ "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') "
			+ "ORDER BY MONTH(bp.created_date)", nativeQuery = true)
	List<Object[]> getMonthlyStocksSold(@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

	@Query(value = "SELECT DATE_FORMAT(created_date, '%b') AS month, " + "SUM(tax) AS count " + "FROM billing_payments "
			+ "WHERE created_date BETWEEN :fromDate AND :toDate "
			+ "GROUP BY MONTH(created_date), DATE_FORMAT(created_date, '%b') "
			+ "ORDER BY MONTH(created_date)", nativeQuery = true)
	List<Object[]> getMonthlyTaxesSummary(@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

}
