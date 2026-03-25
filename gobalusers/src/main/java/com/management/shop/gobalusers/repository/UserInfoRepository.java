package com.management.shop.gobalusers.repository;

import com.management.shop.gobalusers.entity.UserInfo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {

    Optional<UserInfo> findByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE UserInfo u SET u.isActive = true WHERE u.username = :username")
    void updateUserStatus(@Param("username") String username);

    @Query("SELECT u FROM UserInfo u WHERE u.isActive = :isActive AND (u.email = :email OR u.phoneNumber = :phone) ORDER BY u.updatedAt DESC")
    List<UserInfo> validateContact(@Param("email") String email, @Param("phone") String phone, @Param("isActive") boolean isActive);

    @Query("SELECT u FROM UserInfo u WHERE u.phoneNumber = :phone ORDER BY u.updatedAt DESC")
    List<UserInfo> validatePhone(@Param("phone") String phone);

    @Query("SELECT u FROM UserInfo u WHERE u.isActive = :isActive AND (u.email = :email OR u.username = :userId)")
    List<UserInfo> validateUser(@Param("email") String email, @Param("userId") String userId, @Param("isActive") boolean isActive);

    @Query("SELECT u FROM UserInfo u WHERE u.isActive = :isActive AND (u.phoneNumber = :phoneNumber OR u.username = :userId)")
    List<UserInfo> validateUserPhone(@Param("phoneNumber") String phoneNumber, @Param("userId") String userId, @Param("isActive") boolean isActive);

     @Query("SELECT u FROM UserInfo u WHERE u.isActive = :isActive AND (u.phoneNumber = :identifier OR u.username = :identifier OR u.email = :identifier) ORDER BY u.updatedAt DESC")
    UserInfo findFirstByPhoneNumberOrUsernameOrEmail(@Param("identifier") String identifier, @Param("isActive") boolean isActive);
}