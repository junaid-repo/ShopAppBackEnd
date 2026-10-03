package com.management.shop.repository;

import com.management.shop.entity.SupplierPaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPaymentEntity, Integer> {

    Page<SupplierPaymentEntity> findByUserIdOrderByPaymentDateDesc(String userId, Pageable pageable);

    List<SupplierPaymentEntity> findByUserIdAndSupplierIdOrderByPaymentDateDesc(String userId, Integer supplierId);
}
