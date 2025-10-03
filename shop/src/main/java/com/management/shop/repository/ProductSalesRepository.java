package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.management.shop.dto.ProductSalesReport;
import com.management.shop.dto.ProductSalesReportView;
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


}
