package com.management.shop.repository;

import com.management.shop.entity.BillingGstEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BillingGstRepository extends JpaRepository<BillingGstEntity, Integer> {
    void deleteByBillingIdAndUserId(Integer id, String userId);


    @Query(value="select * from billing_gst bg where bg.order_number =?2 and user_id=?1 order by bg.gst_percentage", nativeQuery=true)
    List<BillingGstEntity> findByUserIdAndOrderId(String username, String orderId);


}
