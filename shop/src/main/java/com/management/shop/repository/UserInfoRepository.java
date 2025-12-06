package com.management.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.management.shop.dto.RegisterRequest;
import com.management.shop.entity.UserInfo;

import jakarta.transaction.Transactional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {
    Optional<UserInfo> findByUsername(String username);

	@Modifying
	@Transactional
	@Query(value = "UPDATE user_info SET is_active = true  WHERE username = ?1", nativeQuery = true)
	void updateUserStatus(String username);

	
	@Query(value = "SELECT   * FROM    user_info WHERE  is_active=?3 and (    email=?1 or phone_number=?2)", nativeQuery = true)
	List<UserInfo> validateContact(String email, String phone, boolean isActive);

    @Query(value = "SELECT   * FROM    user_info WHERE  is_active=?3 and (    email=?1 or username=?2)", nativeQuery = true)
    List<UserInfo> validateUser(String email, String userId, boolean b);

    @Query(value = "SELECT   * FROM    user_info WHERE  is_active=?1", nativeQuery = true)
    List<UserInfo> findAllByStatus(Boolean aTrue);

    @Modifying
    @Transactional
    @Query(value = "UPDATE user_info SET roles = ?2  WHERE username = ?1 and is_active=true", nativeQuery = true)
    void updateUserRole(String s, String rolePremium);

    @Query(value = "SELECT   * FROM    user_info WHERE  is_active=?1 and roles=?2", nativeQuery = true)
    List<UserInfo> findAllByStatusAndRole(Boolean aTrue, String rolePremium);
}