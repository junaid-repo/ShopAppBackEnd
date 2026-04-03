package com.management.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.management.shop.entity.UserInfoStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserInfoStatusRepository extends JpaRepository<UserInfoStatus, Integer> {
    UserInfoStatus findByUsername(String s);

    @Query(value="Update UserInfoStatus uis set uis.status =:status, uis.updatedAt =:updatedDate, uis.reason =:reason where uis.username =:username")
    void updateUserStatus(@Param("username")String username, @Param("updatedDate") LocalDateTime now, @Param("status") String inactive, @Param("reason") String reason);
}
