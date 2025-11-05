package com.management.shop.repository;

import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.ShopUPIEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ShopUPIRepository extends JpaRepository<ShopUPIEntity, Integer>
{

    @Query(value="select * from shop_upi where user_id=?1 order by updated_at desc limit 1", nativeQuery=true)
    ShopUPIEntity findByShopFinanceId(String id);

    @Modifying
    @Transactional
    @Query(value="delete from shop_upi where user_id=?1", nativeQuery=true)
    void removeUpiId(String s);
}
