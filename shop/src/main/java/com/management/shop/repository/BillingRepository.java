package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = "SELECT DATE_FORMAT(bp.created_date, '%a') AS day, " +
            "SUM(ps.quantity) AS totalStocksSold " +
            "FROM billing_payments bp " +
            "JOIN product_sales ps ON bp.id = ps.billing_id " +
            "WHERE bp.created_date BETWEEN DATE_SUB(:currentDateMax, INTERVAL 6 DAY) AND :currentDateMax " +
            "AND bp.user_id = :userId " +
            "GROUP BY DATE(bp.created_date), DATE_FORMAT(bp.created_date, '%a') " +
            "ORDER BY DATE(bp.created_date)",
            nativeQuery = true)
    List<Object[]> getWeeklyStocksSold(@Param("fromDate") LocalDateTime fromDate,
                                        @Param("currentDateMax") LocalDateTime currentDateMax,
                                        @Param("userId") String userId);



    @Query(value = "WITH RECURSIVE days AS ( " +
            "  SELECT DATE(DATE_SUB(:currentDateMax, INTERVAL 6 DAY)) AS d " +
            "  UNION ALL " +
            "  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE(:currentDateMax) " +
            ") " +
            "SELECT DATE_FORMAT(d, '%a') AS day, " +
            "COALESCE(SUM(bp.total), 0) AS count, " +
            "COALESCE(SUM(bd.total_profit_oncp), 0) AS totalProfit " +
            "FROM days " +
            "LEFT JOIN billing_payments bp ON DATE(bp.created_date) = d AND bp.user_id = :userId " +
            "LEFT JOIN billing_details bd ON bp.billing_id = bd.id " +
            "GROUP BY d " +
            "ORDER BY d",
            nativeQuery = true)
    List<Object[]> getWeeklySalesSummary(@Param("fromDate") LocalDateTime fromDate,
                                          @Param("currentDateMax") LocalDateTime currentDateMax,
                                          @Param("userId") String userId);

    @Query(value = "WITH RECURSIVE days AS ( " +
            "  SELECT DATE(DATE_SUB(:currentDateMax, INTERVAL 6 DAY)) AS d " +
            "  UNION ALL " +
            "  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE(:currentDateMax) " +
            "), " +
            "daily_sales AS ( " +
            "  SELECT " +
            "    DATE(bp.created_date) AS sale_date, " +
            "    SUM(bd.total_amount) AS daily_total, " +
            "    SUM(bd.total_profit_oncp) AS daily_profit, " +
            "    SUM(bd.units_sold) AS daily_stocks_sold " + // Added this line
            "  FROM billing_payments bp " +
            "  JOIN billing_details bd ON bp.billing_id = bd.id " +
            "  WHERE bp.user_id = :userId AND bp.created_date >= DATE_SUB(:currentDateMax, INTERVAL 7 DAY) " +
            "  GROUP BY sale_date " +
            ") " +
            "SELECT " +
            "  DATE_FORMAT(d.d, '%a') AS day, " +
            "  COALESCE(ds.daily_total, 0) AS count, " +
            "  COALESCE(ds.daily_profit, 0) AS totalProfit, " +
            "  COALESCE(ds.daily_stocks_sold, 0) AS totalStocksSold " + // Updated to use 'ds' alias
            "FROM days d " +
            "LEFT JOIN daily_sales ds ON d.d = ds.sale_date " + // Removed the join to daily_stocks
            "ORDER BY d.d",
            nativeQuery = true)
    List<Object[]> getWeeklySalesAndStocks(@Param("currentDateMax") LocalDateTime currentDateMax,
                                           @Param("userId") String userId);

    @Query(value = "WITH RECURSIVE days AS ( " +
            "  SELECT DATE(DATE_SUB(:currentDateMax, INTERVAL 6 DAY)) AS d " +
            "  UNION ALL " +
            "  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE(:currentDateMax) " +
            "), " +
            "sales AS ( " +
            "  SELECT " +
            "    DATE(bp.created_date) AS sale_date, " +
            "    SUM(bd.total_amount) AS daily_total, " +
            "    SUM(bd.total_profit_oncp) AS daily_profit " +
            "  FROM billing_payments bp " +
            "  JOIN billing_details bd ON bp.billing_id = bd.id " +
            "  WHERE bp.user_id = :userId AND bp.created_date >= DATE_SUB(:currentDateMax, INTERVAL 7 DAY) " +
            "  GROUP BY sale_date " +
            "), " +
            "stocks AS ( " +
            "  SELECT " +
            "    DATE(bp.created_date) AS sale_date, " +
            "    SUM(ps.quantity) AS daily_stocks_sold " +
            "  FROM billing_payments bp " +
            "  JOIN product_sales ps ON bp.id = ps.billing_id " +
            "  WHERE bp.user_id = :userId AND bp.created_date >= DATE_SUB(:currentDateMax, INTERVAL 7 DAY) " +
            "  GROUP BY sale_date " +
            ") " +
            "SELECT " +
            "  DATE_FORMAT(d.d, '%a') AS day, " +
            "  COALESCE(ds.daily_total, 0) AS count, " +
            "  COALESCE(ds.daily_profit, 0) AS totalProfit, " +
            "  COALESCE(d_stocks.daily_stocks_sold, 0) AS totalStocksSold " +
            "FROM days d " +
            "LEFT JOIN daily_sales ds ON d.d = ds.sale_date " +
            "LEFT JOIN daily_stocks d_stocks ON d.d = d_stocks.sale_date " +
            "ORDER BY d.d",
            nativeQuery = true)
    List<Object[]> getWeeklySalesAndStocksWeekly(@Param("currentDateMax") LocalDateTime currentDateMax,
                                           @Param("userId") String userId);


    @Query(value = "WITH time_slots AS ( " +
            "  SELECT 0 AS start_hour, '12 AM - 4 AM' AS slot_label UNION ALL " +
            "  SELECT 4,  '4 AM - 8 AM'  UNION ALL " +
            "  SELECT 8,  '8 AM - 12 PM' UNION ALL " +
            "  SELECT 12, '12 PM - 4 PM' UNION ALL " +
            "  SELECT 16, '4 PM - 8 PM'  UNION ALL " +
            "  SELECT 20, '8 PM - 12 AM' " +
            "), " +
            "hourly_sales AS ( " +
            "  SELECT " +
            "    CASE " +
            "      WHEN HOUR(bp.created_date) BETWEEN 0 AND 3 THEN 0 " +
            "      WHEN HOUR(bp.created_date) BETWEEN 4 AND 7 THEN 4 " +
            "      WHEN HOUR(bp.created_date) BETWEEN 8 AND 11 THEN 8 " +
            "      WHEN HOUR(bp.created_date) BETWEEN 12 AND 15 THEN 12 " +
            "      WHEN HOUR(bp.created_date) BETWEEN 16 AND 19 THEN 16 " +
            "      ELSE 20 " +
            "    END AS slot_start_hour, " +
            "    SUM(bd.total_amount) AS hourly_total, " +
            "    SUM(bd.total_profit_oncp) AS hourly_profit, " +
            "    SUM(bd.units_sold) AS hourly_stocks_sold " + // Fetches stocks from billing_details
            "  FROM billing_payments bp " +
            "  JOIN billing_details bd ON bp.billing_id = bd.id " +
            "  WHERE bp.user_id = :userId AND DATE(bp.created_date) = DATE(:currentDate) " +
            "  GROUP BY slot_start_hour " +
            ") " +
            "SELECT " +
            "  ts.slot_label AS timeOfDay, " +
            "  COALESCE(hs.hourly_total, 0) AS count, " +
            "  COALESCE(hs.hourly_profit, 0) AS totalProfit, " +
            "  COALESCE(hs.hourly_stocks_sold, 0) AS totalStocksSold " + // Uses the new column from hourly_sales
            "FROM time_slots ts " +
            "LEFT JOIN hourly_sales hs ON ts.start_hour = hs.slot_start_hour " +
            "ORDER BY ts.start_hour",
            nativeQuery = true)
    List<Object[]> getSalesAndStocksToday(@Param("currentDate") LocalDateTime currentDate,
                                          @Param("userId") String userId);

    @Query(value = "WITH RECURSIVE weeks (week_end_date) AS ( " +
            "  SELECT DATE(:currentDateMax) " +
            "  UNION ALL " +
            "  SELECT DATE(DATE_SUB(week_end_date, INTERVAL 1 WEEK)) " +
            "  FROM weeks " +
            "  WHERE DATE_SUB(week_end_date, INTERVAL 1 WEEK) >= DATE_SUB(:currentDateMax, INTERVAL 1 MONTH) " +
            "), " +
            "weekly_sales AS ( " +
            "  SELECT " +
            "    w.week_end_date, " +
            "    SUM(bd.total_amount) AS total, " +
            "    SUM(bd.total_profit_oncp) AS profit, " +
            "    SUM(bd.units_sold) AS stocks_sold " +
            "  FROM weeks w " +
            "  JOIN billing_payments bp ON DATE(bp.created_date) BETWEEN DATE_SUB(w.week_end_date, INTERVAL 6 DAY) AND w.week_end_date " +
            "  JOIN billing_details bd ON bp.billing_id = bd.id " +
            "  WHERE bp.user_id = :userId " +
            "  GROUP BY w.week_end_date " +
            ") " +
            "SELECT " +
            "  DATE_FORMAT(w.week_end_date, '%b %d') AS period, " +
            "  COALESCE(ws.total, 0) AS count, " +
            "  COALESCE(ws.profit, 0) AS totalProfit, " +
            "  COALESCE(ws.stocks_sold, 0) AS totalStocksSold " +
            "FROM weeks w " +
            "LEFT JOIN weekly_sales ws ON w.week_end_date = ws.week_end_date " +
            "ORDER BY w.week_end_date ASC",
            nativeQuery = true)
    List<Object[]> getSalesAndStocksMonthly(@Param("currentDateMax") LocalDateTime currentDateMax,
                                            @Param("userId") String userId);

    @Query(value = "WITH RECURSIVE months (month_start) AS ( " +
            "  SELECT DATE(DATE_FORMAT(DATE_SUB(:currentDateMax, INTERVAL 11 MONTH), '%Y-%m-01')) " +
            "  UNION ALL " +
            "  SELECT DATE_ADD(month_start, INTERVAL 1 MONTH) FROM months WHERE month_start < DATE(DATE_FORMAT(:currentDateMax, '%Y-%m-01')) " +
            "), " +
            "monthly_sales AS ( " +
            "  SELECT " +
            "    DATE(DATE_FORMAT(bp.created_date, '%Y-%m-01')) AS month_period, " +
            "    SUM(bd.total_amount) AS total, " +
            "    SUM(bd.total_profit_oncp) AS profit, " +
            "    SUM(bd.units_sold) AS stocks_sold " + // Added this line
            "  FROM billing_payments bp " +
            "  JOIN billing_details bd ON bp.billing_id = bd.id " +
            "  WHERE bp.user_id = :userId " +
            "    AND bp.created_date >= DATE(DATE_FORMAT(DATE_SUB(:currentDateMax, INTERVAL 11 MONTH), '%Y-%m-01')) " +
            "    AND bp.created_date <= :currentDateMax " +
            "  GROUP BY month_period " +
            ") " +
            "SELECT " +
            "  DATE_FORMAT(m.month_start, '%b %Y') AS period, " +
            "  COALESCE(ms.total, 0) AS count, " +
            "  COALESCE(ms.profit, 0) AS totalProfit, " +
            "  COALESCE(ms.stocks_sold, 0) AS totalStocksSold " + // Updated to use 'ms' alias
            "FROM months m " +
            "LEFT JOIN monthly_sales ms ON m.month_start = ms.month_period " + // Removed the second join
            "ORDER BY m.month_start ASC",
            nativeQuery = true)
    List<Object[]> getSalesAndStocksYearly(@Param("currentDateMax") LocalDateTime currentDateMax,
                                           @Param("userId") String userId);

    @Query(
            value = "SELECT * " +
                    "FROM billing_details " +
                    "WHERE user_id = ?1 " +
                    "AND DATE(created_date) = CURRENT_DATE " +
                    "ORDER BY total_amount DESC " +
                    "LIMIT ?2",
            nativeQuery = true
    )
    List<BillingEntity> findTopNSalesForToday(String userId, int count);

    @Query(
            value = "SELECT * " +
                    "FROM billing_details " +
                    "WHERE user_id = ?1 " +
                    "AND created_date >= CURDATE() - INTERVAL 7 DAY " +
                    "AND created_date < CURDATE() + INTERVAL 1 DAY " +
                    "ORDER BY total_amount DESC " +
                    "LIMIT ?2",
            nativeQuery = true
    )
    List<BillingEntity> findTopNSalesForLastWeek(String userId, int count);

    @Query(
            value = "SELECT * " +
                    "FROM billing_details " +
                    "WHERE user_id = ?1 " +
                    "AND created_date >= CURDATE() - INTERVAL 1 MONTH " +
                    "AND created_date < CURDATE() + INTERVAL 1 DAY " +
                    "ORDER BY total_amount DESC " +
                    "LIMIT ?2",
            nativeQuery = true
    )
    List<BillingEntity> findTopNSalesForLastMonth(String userId, int count);

    @Query(
            value = "SELECT * " +
                    "FROM billing_details " +
                    "WHERE user_id = ?1 " +
                    "AND created_date >= CURDATE() - INTERVAL 1 YEAR " +
                    "AND created_date < CURDATE() + INTERVAL 1 DAY " +
                    "ORDER BY total_amount DESC " +
                    "LIMIT ?2",
            nativeQuery = true
    )
    List<BillingEntity> findTopNSalesForLastYear(String userId, int count);

    @Query(
            value = "SELECT * " +
                    "FROM billing_details " +
                    "WHERE user_id = :userId " +
                    "AND created_date >= :fromDate " +
                    "AND created_date < :toDate " +
                    "ORDER BY total_amount DESC ",
            nativeQuery = true
    )
    List<BillingEntity> findSalesNDays(
            @Param("userId") String userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );



    @Query(
            value = """
            SELECT *
            FROM billing_details
            WHERE user_id = :userId
              AND created_date BETWEEN :startDate AND :endDate
            ORDER BY total_amount DESC
            LIMIT :count
            """,
            nativeQuery = true
    )
    List<BillingEntity> findTopNSalesForGivenRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("count") int count
    );

    BillingEntity findByInvoiceNumber(String orderNo);

    @Transactional
    @Modifying
    @Query(value="update billing_details set due_reminder_count= due_reminder_count+1, updated_by=?2, updated_date=?3 where invoice_number=?1 and user_id=?2", nativeQuery = true)
    void updateReminderCount(String orderNo, String username, LocalDateTime updatedDate);

    @Transactional
    @Modifying
    @Query(value="update billing_details set paying_amount=paying_amount+?4, remaining_amount= remaining_amount-?4, updated_by=?2, updated_date=?3 where invoice_number=?1 and user_id=?2", nativeQuery = true)
    void updateDuePayment(String orderNo, String username, LocalDateTime updatedDate, Double amount);

    @Query(value="select count(*) from billing_details where user_id=?1 and created_date BETWEEN ?2 AND ?3", nativeQuery = true)
    Integer countOrdersForToday(String username, LocalDateTime localDateTime, LocalDateTime localDateTime1);




    @Query(value = "SELECT " +
            "DATE_FORMAT(bp.created_date, '%b') AS month, " +
            "SUM(bd.total_profit_oncp) AS totalProfit, " +
            "SUM(bd.total_amount) AS totalRevenue, " +
            "SUM(bd.units_sold) AS totalStock, " +
            "COUNT(bp.id) AS salesCount " +
            "FROM billing_payments bp " +
            "JOIN billing_details bd ON bp.billing_id = bd.id " +
            "WHERE bp.created_date BETWEEN :fromDate AND :toDate " +
            "AND bp.user_id = :userId " +
            "GROUP BY MONTH(bp.created_date), DATE_FORMAT(bp.created_date, '%b') " +
            "ORDER BY MONTH(bp.created_date)",
            nativeQuery = true)
    List<Object[]> getMonthlyBillingSummary(@Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDate") LocalDateTime toDate,
                                            @Param("userId") String userId);
}
