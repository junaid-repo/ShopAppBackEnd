package com.management.shop.repository;

import com.management.shop.entity.BillingGstEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BillingGstRepository extends JpaRepository<BillingGstEntity, Integer> {
    void deleteByBillingIdAndUserId(Integer id, String userId);


    @Query(value="select * from billing_gst bg where bg.order_number =?2 and user_id=?1 order by bg.gst_percentage", nativeQuery=true)
    List<BillingGstEntity> findByUserIdAndOrderId(String username, String orderId);



    @Transactional
    @Modifying
    @Query("UPDATE BillingGstEntity b set b.orderNumber= :cancelledOrderNumber, b.updatedDate = : updatedDate, b.updatedBy = :username where b.orderNumber =: orderNumber and b.userId =: username")
    void updateCancelledOrder(@Param("orderNumber") String orderNumber,@Param("cancelledOrderNumber") String cancelledOrderNumber,@Param("username") String username,@Param("updatedDate") LocalDateTime updatedDate);

}
