package com.management.shop.repository;

import com.management.shop.entity.SupplierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Integer> {

    List<SupplierEntity> findByUserIdAndIsActiveTrueOrderByNameAsc(String userId);

    Optional<SupplierEntity> findByIdAndUserId(Integer id, String userId);

    @Query("SELECT s FROM SupplierEntity s WHERE s.userId = :userId " +
           "AND (:search = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY s.name ASC")
    Page<SupplierEntity> searchSuppliers(@Param("userId") String userId,
                                         @Param("search") String search,
                                         Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(balance_due), 0) FROM shop_supplier WHERE user_id = :userId AND is_active = 1", nativeQuery = true)
    Double getTotalSupplierDues(@Param("userId") String userId);
}
