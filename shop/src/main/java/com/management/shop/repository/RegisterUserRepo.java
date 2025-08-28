package com.management.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.management.shop.entity.RegisterUserOTPEntity;

import jakarta.transaction.Transactional;

public interface RegisterUserRepo extends JpaRepository<RegisterUserOTPEntity, Integer>{

	RegisterUserOTPEntity getByUsername(String storedUser);

	@Modifying
	@Transactional
	@Query(value = "UPDATE newuo  SET status = ?2 WHERE id = ?1", nativeQuery = true)
	void updateOldOTP(Integer id, String status);

}
