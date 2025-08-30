package com.management.shop.gobalusers.repository;

import com.management.shop.gobalusers.entity.UserProfilePicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfilePicRepo extends JpaRepository<UserProfilePicEntity, Integer>{

	UserProfilePicEntity findByUsername(String username);

}
