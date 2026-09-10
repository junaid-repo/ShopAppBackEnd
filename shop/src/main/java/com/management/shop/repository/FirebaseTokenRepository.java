package com.management.shop.repository;

import com.management.shop.entity.FirebaseTokenEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FirebaseTokenRepository extends JpaRepository<FirebaseTokenEntity, Integer> {
    
    FirebaseTokenEntity findByUsernameAndDeviceType(String username, String deviceType);

    FirebaseTokenEntity findTopByUsernameOrderByLastUpdatedDateDesc(String username);

    @Query("SELECT fte.firebaseToken FROM FirebaseTokenEntity fte WHERE fte.username = ?1")
    List<String> findAllTokenByUsername(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM FirebaseTokenEntity fte WHERE fte.firebaseToken = ?1")
    void deleteByFirebaseToken(String deadToken);
}
