package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.ProductEntity;

import jakarta.transaction.Transactional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_product SET stock = stock - ?2, status = CASE    WHEN stock - ?2 <= 0 THEN 'Out of Stock' ELSE status END WHERE id = ?1 AND stock > 0", nativeQuery = true)
	void updateProductStock(Integer id, Integer quantity);

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_product SET stock = stock + ?2,  status = CASE  WHEN stock + ?2 > 0 THEN 'In Stock' ELSE status END WHERE id = ?1 AND stock > 0", nativeQuery = true)
	void addProductStock(Integer id, Integer quantity);

	/*
	 * @Query(value =
	 * "select * from billing_details where created_date BETWEEN ?1 AND ?2",
	 * nativeQuery = true) List<ProductEntity> findProductsByDateRage(LocalDateTime
	 * fromDate, LocalDateTime toDate);
	 */
	@Query(value = "select * from shop_product where active=?1", nativeQuery = true)
	List<ProductEntity> findAllByStatus(Boolean isActive);

	@Query(value ="SELECT * FROM shop_product WHERE created_date >= ?1 	   AND created_date < ?2", nativeQuery=true)
	List<ProductEntity> findAllCreatedToday( LocalDateTime startOfDay,
			LocalDateTime endOfDay);

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_product SET active = ?2 where id=?1", nativeQuery = true)
	void deActivateProduct(Integer id, Boolean isActive);
	
	
	@Query(value = "select * from shop_product where active=?1", nativeQuery = true)
	List<ProductEntity> findAllActiveProducts(Boolean isActive);
}
