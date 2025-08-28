package com.management.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.management.shop.entity.UserProfilePicEntity;

public interface UserProfilePicRepo extends JpaRepository<UserProfilePicEntity, Integer>{

	UserProfilePicEntity findByUsername(String username);

}
