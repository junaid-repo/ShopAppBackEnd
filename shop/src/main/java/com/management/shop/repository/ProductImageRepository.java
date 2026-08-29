package com.management.shop.repository;

import com.management.shop.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
    Optional<ProductImageEntity> findByProductIdAndUserId(Integer productId, String userId);
    void deleteByProductIdAndUserId(Integer productId, String userId);
}
