package com.management.shop.repository;

import com.management.shop.entity.UserPaymentModes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UserPaymentModesRepo extends JpaRepository<UserPaymentModes, Integer> {

    @Query(value="Select * from user_payment_modes where user_id=?1", nativeQuery = true)
    UserPaymentModes getUserPaymentModes(String userId);


}
