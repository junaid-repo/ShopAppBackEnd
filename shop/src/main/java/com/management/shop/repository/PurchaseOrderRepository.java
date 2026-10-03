package com.management.shop.repository;

import com.management.shop.entity.PurchaseOrderEntity;
import com.management.shop.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Integer> {

    Optional<PurchaseOrderEntity> findByIdAndUserId(Integer id, String userId);

    Optional<PurchaseOrderEntity> findByPoNumberAndUserId(String poNumber, String userId);

    @Query(value = "SELECT p.* FROM purchase_orders p " +
            "JOIN shop_supplier s ON p.supplier_id = s.id " +
            "WHERE p.user_id = :userId " +
            "AND (:status IS NULL OR p.order_status = :status) " +
            "AND (p.order_date BETWEEN :fromDate AND :toDate) " +
            "AND (:search = '' " +
            "  OR LOWER(p.po_number) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(COALESCE(p.supplier_invoice_number, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.order_date DESC, p.id DESC",
            countQuery = "SELECT COUNT(*) FROM purchase_orders p " +
            "JOIN shop_supplier s ON p.supplier_id = s.id " +
            "WHERE p.user_id = :userId " +
            "AND (:status IS NULL OR p.order_status = :status) " +
            "AND (p.order_date BETWEEN :fromDate AND :toDate) " +
            "AND (:search = '' " +
            "  OR LOWER(p.po_number) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(COALESCE(p.supplier_invoice_number, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<PurchaseOrderEntity> findPurchaseOrders(@Param("userId") String userId,
                                                @Param("status") String status,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate,
                                                @Param("search") String search,
                                                Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM purchase_orders " +
            "WHERE user_id = :userId AND order_status != 'CANCELLED'", nativeQuery = true)
    Double getTotalPurchasesSum(@Param("userId") String userId);

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN due_amount IS NOT NULL THEN due_amount ELSE GREATEST(0, COALESCE(total_amount, 0) - COALESCE(paid_amount, 0)) END), 0) FROM purchase_orders " +
            "WHERE user_id = :userId AND order_status != 'CANCELLED'", nativeQuery = true)
    Double getTotalDueAmountSum(@Param("userId") String userId);

    @Query(value = "SELECT COUNT(*) FROM purchase_orders " +
            "WHERE user_id = :userId AND order_status != 'CANCELLED'", nativeQuery = true)
    Integer getTotalOrdersCount(@Param("userId") String userId);

    @Query(value = "SELECT COUNT(*) FROM purchase_orders " +
            "WHERE user_id = :userId AND order_status IN ('ORDERED', 'PARTIALLY_RECEIVED')", nativeQuery = true)
    Integer getPendingOrdersCount(@Param("userId") String userId);

    @Query(value = "SELECT COALESCE(MAX(id), 0) FROM purchase_orders WHERE user_id = :userId", nativeQuery = true)
    Integer getMaxIdForUser(@Param("userId") String userId);
}
