package com.management.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.management.shop.entity.ShopDetailsEntity;

public interface ShopDetailsRepo extends JpaRepository<ShopDetailsEntity, Integer>{

	@Query(value="select * from shop_details_entity where username=?1", nativeQuery=true)
	ShopDetailsEntity findbyUsername(String username);

}
