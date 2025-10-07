package com.management.shop.repository;

import com.management.shop.entity.ShopBasicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopBasicRepository extends JpaRepository<ShopBasicEntity, Integer> {

    @Query(value="select * from shop_basic where user_id=?1 order by updated_at desc limit 1", nativeQuery=true)
    ShopBasicEntity findByUserId(String username);
}
