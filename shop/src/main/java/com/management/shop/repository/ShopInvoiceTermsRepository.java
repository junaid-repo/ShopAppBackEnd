package com.management.shop.repository;

import com.management.shop.entity.ShopFinanceEntity;
import com.management.shop.entity.ShopInvoiceTermsEnity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopInvoiceTermsRepository extends JpaRepository<ShopInvoiceTermsEnity, Integer> {

    @Query(value="select * from shop_inovice_terms where user_id=?1 order by updated_at desc limit 1", nativeQuery=true)
    ShopInvoiceTermsEnity findByUserId(String username);
}
