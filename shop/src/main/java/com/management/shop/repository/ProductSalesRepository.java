package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.management.shop.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.management.shop.entity.ProductSalesEntity;
import org.springframework.data.repository.query.Param;

public interface ProductSalesRepository extends JpaRepository<ProductSalesEntity, Integer>{

	@Query(value="select * from product_sales where billing_id=?1 and user_id=?2", nativeQuery=true)
	List<ProductSalesEntity> findByOrderId(Integer id, String userId);

    @Query(value = "SELECT sp.name AS productName, " +
            "       sp.category AS category, " +
            "       SUM(ps.quantity) AS totalSold, " +
            "       ps.total AS total, " +
            "       ps.tax AS tax, " +
            "       ROUND(ps.profit_oncp, 2) AS profitOnCp, " +
            "       bd.invoice_number AS invoiceNumber, " +
            "       bd.created_date AS invoiceDate " +
            "FROM product_sales ps " +
            "JOIN billing_details bd ON ps.billing_id = bd.id " +
            "JOIN shop_product sp ON ps.product_id = sp.id " +
            "WHERE bd.created_date BETWEEN :fromDate AND :toDate " +
            "  AND bd.user_id = :userId " +
            "GROUP BY sp.id, sp.name, sp.category, ps.total, ps.tax, bd.invoice_number " +
            "ORDER BY invoiceDate DESC",
            nativeQuery = true)
    List<ProductSalesReportView> findSalesReportNative(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("userId") String userId);


    @Query(value = """
            SELECT
                sp.name as productName,
                sp.category as category,
                SUM(ps.quantity) as unitsSold,
                SUM(ps.total) as revenue,
                sp.stock as currentStock
            FROM product_sales ps
            JOIN shop_product sp ON ps.product_id = sp.id
            JOIN billing_details bd ON ps.billing_id = bd.id
            WHERE ps.user_id = :userId
              AND bd.created_date BETWEEN :startDate AND :endDate
            GROUP BY sp.id, sp.name, sp.category, sp.stock
            ORDER BY unitsSold DESC
            LIMIT :count
            """, nativeQuery = true)
    List<ProductPerformanceProjection> findMostSellingProducts(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("count") int count);

    /**
     * Finds the top grossing products based on the total revenue generated.
     */
    @Query(value = """
            SELECT
                sp.name as productName,
                sp.category as category,
                SUM(ps.quantity) as unitsSold,
                SUM(ps.total) as revenue,
                sp.stock as currentStock
            FROM product_sales ps
            JOIN shop_product sp ON ps.product_id = sp.id
            JOIN billing_details bd ON ps.billing_id = bd.id
            WHERE ps.user_id = :userId
              AND bd.created_date BETWEEN :startDate AND :endDate
            GROUP BY sp.id, sp.name, sp.category, sp.stock
            ORDER BY revenue DESC
            LIMIT :count
            """, nativeQuery = true)
    List<ProductPerformanceProjection> findTopGrossingProducts(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("count") int count);


    @Query(value = "SELECT sp.name AS productName, " +
            "SUM(ps.quantity) AS totalQuantity " +
            "FROM product_sales ps " +
            "JOIN billing_details bd ON ps.billing_id = bd.id " +
            "JOIN shop_product sp ON ps.product_id = sp.id " + // <-- 1. JOINED shop_product
            "WHERE bd.created_date BETWEEN :fromDate AND :toDate " +
            "AND bd.user_id = :userId " +
            "AND sp.name IS NOT NULL AND sp.name != '' " + // <-- 2. FILTERED null/empty names
            "GROUP BY sp.name " + // <-- Group by the REAL name
            "ORDER BY totalQuantity DESC " +
            "LIMIT :n",
            nativeQuery = true)
    List<Object[]> getTopSoldProducts(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("userId") String userId,
            @Param("n") Integer n
    );
    @Query(value = "SELECT " +
            "    p.hsn as hsn, " +
            "    p.name as productName, " +
            "    SUM(ps.quantity) as totalQuantity, " +
            "    SUM(ps.sub_total) as totalTaxableValue, " +
            "    SUM(ps.cgst) as totalCgst, " +
            "    SUM(ps.sgst) as totalSgst, " +
            "    SUM(ps.igst) as totalIgst, " +
            "    SUM(ps.tax) as totalGst " +
            "FROM " +
            "    product_sales ps " +
            "JOIN " +
            "    shop_product p ON ps.product_id = p.id AND ps.user_id = p.user_id " +
            "JOIN " +
            "    billing_details b ON ps.billing_id = b.id AND ps.user_id = b.user_id " +
            "WHERE " +
            "    b.created_date BETWEEN ?1 AND ?2 " +
            "    AND ps.user_id = ?3 " +
            "GROUP BY " +
            "    p.hsn, p.name " +
            "ORDER BY " +
            "    p.hsn", nativeQuery = true)
    List<GstByHsnDto> findGstByHsn(LocalDateTime fromDate, LocalDateTime toDate, String userId);

    /**
     * 3) Finds monthly GST summary for the date range.
     */
    @Query(value = "SELECT " +
            "    DATE_FORMAT(b.created_date, '%Y-%m') as monthYear, " +
            "    SUM(ps.sub_total) as totalTaxableValue, " +
            "    SUM(ps.cgst) as totalCgst, " +
            "    SUM(ps.sgst) as totalSgst, " +
            "    SUM(ps.igst) as totalIgst, " +
            "    SUM(ps.tax) as totalGst " +
            "FROM " +
            "    product_sales ps " +
            "JOIN " +
            "    billing_details b ON ps.billing_id = b.id AND ps.user_id = b.user_id " +
            "WHERE " +
            "    b.created_date BETWEEN ?1 AND ?2 " +
            "    AND ps.user_id = ?3 " +
            "GROUP BY " +
            "    DATE_FORMAT(b.created_date, '%Y-%m') " +
            "ORDER BY " +
            "    monthYear ASC", nativeQuery = true)
    List<MonthlyGstSummaryDto> findMonthlyGstSummary(LocalDateTime fromDate, LocalDateTime toDate, String userId);
}
