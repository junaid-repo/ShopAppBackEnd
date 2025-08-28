package com.management.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.management.shop.entity.ProductSalesEntity;

public interface ProductSalesRepository extends JpaRepository<ProductSalesEntity, Integer>{

	@Query(value="select * from product_sales where billing_id=?1", nativeQuery=true)
	List<ProductSalesEntity> findByOrderId(Integer id);

}
