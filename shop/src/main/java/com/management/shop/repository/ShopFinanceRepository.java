package com.management.shop.repository;

import com.management.shop.entity.ShopBasicEntity;
import com.management.shop.entity.ShopFinanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopFinanceRepository extends JpaRepository<ShopFinanceEntity, Integer> {

    @Query(value="select * from shop_finance where user_id=?1 order by updated_at desc limit 1", nativeQuery=true)
    ShopFinanceEntity findByUserId(String username);
}
