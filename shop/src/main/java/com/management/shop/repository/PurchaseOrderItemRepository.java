package com.management.shop.repository;

import com.management.shop.entity.PurchaseOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItemEntity, Integer> {

    List<PurchaseOrderItemEntity> findByPurchaseOrderId(Integer purchaseOrderId);

    @Query(value = "SELECT poi.* FROM purchase_order_items poi " +
            "JOIN purchase_orders po ON poi.purchase_order_id = po.id " +
            "WHERE po.user_id = :userId AND po.supplier_id = :supplierId " +
            "ORDER BY poi.id DESC LIMIT :limit", nativeQuery = true)
    List<PurchaseOrderItemEntity> findRecentItemsBySupplier(@Param("userId") String userId,
                                                            @Param("supplierId") Integer supplierId,
                                                            @Param("limit") int limit);
}
