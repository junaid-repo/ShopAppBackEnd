package com.management.shop.repository;

import com.management.shop.entity.UserSubscriptions;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserSubscriptionsRepository  extends JpaRepository<UserSubscriptions, Integer> {
    UserSubscriptions findBySubscriptionId(String subscriptionId);

    @Query(value="select * from user_subscriptions where username = ?1 and status= ?2 order by id desc limit 1", nativeQuery = true)
    UserSubscriptions findByUsername(String s, String status);

    @Query(value="select * from user_subscriptions where username = ?1 and status!= ?2 order by id", nativeQuery = true)
    List<UserSubscriptions> findByUsernameList(String s, String status);

    @Query(value = "SELECT * FROM user_subscriptions WHERE username = ?1 AND status IN ('active', 'upcoming') ORDER BY id DESC LIMIT 1", nativeQuery = true)
    UserSubscriptions findLatestActiveOrUpcomingByUsername(String username);

    @Query(value="select * from user_subscriptions where username = ?1 and status!= ?2 order by id", nativeQuery = true)
    List<UserSubscriptions> findSubListByUsername(String username, String status);


    @Modifying
    @Transactional
    @Query(value="update user_subscriptions set status = ?1, updated_at=NOW()  where id = ?2", nativeQuery = true)
    void updateById(String expired, Integer id);
}
