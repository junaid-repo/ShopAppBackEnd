package com.management.shop.repository;

import com.management.shop.entity.ShopBankEntity;
import com.management.shop.entity.ShopBasicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopBankRepository extends JpaRepository<ShopBankEntity, Integer> {

    @Query(value="select * from shop_banks where shop_finance_id=?1 order by updated_at desc limit 1", nativeQuery=true)
    ShopBankEntity findByShopFinanceId(Integer id);
}
