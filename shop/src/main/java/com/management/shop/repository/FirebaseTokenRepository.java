package com.management.shop.repository;

import com.management.shop.entity.FirebaseTokenEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface FirebaseTokenRepository extends JpaRepository<FirebaseTokenEntity, Integer> {
    
    @Query("SELECT fte FROM FirebaseTokenEntity fte WHERE fte.firebaseToken = ?1 AND fte.username = ?2")
    FirebaseTokenEntity findByDeviceIdAndUsername(String deviceType, String s);

    FirebaseTokenEntity findTopByUsernameOrderByLastUpdatedDateDesc(String username);

    @Transactional
    @Modifying
    @Query("UPDATE FirebaseTokenEntity fte SET fte.firebaseToken = ?1, fte.lastUpdatedBy = ?2, fte.lastUpdatedDate = ?3 WHERE fte.username = ?4")
    void updateExistingToken(String token, String s, LocalDateTime now);
}
