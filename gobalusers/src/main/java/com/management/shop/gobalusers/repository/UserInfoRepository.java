package com.management.shop.gobalusers.repository;

import com.management.shop.gobalusers.entity.UserInfo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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



}