package com.management.shop.repository;

import com.management.shop.entity.ShopBankEntity;
import com.management.shop.entity.ShopBasicEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ShopBankRepository extends JpaRepository<ShopBankEntity, Integer> {

    @Query(value="select * from shop_banks where user_id=?1 order by updated_at desc limit 1", nativeQuery=true)
    ShopBankEntity findByShopFinanceId(String username);

    @Transactional
    @Modifying
    @Query(value="delete from shop_banks where user_id=?1", nativeQuery=true)
    void removeBankDetails(String s);
}
