package com.management.shop.repository;

import com.management.shop.entity.FirebaseTokenEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FirebaseTokenRepository extends JpaRepository<FirebaseTokenEntity, Integer> {
    
    @Query("SELECT fte FROM FirebaseTokenEntity fte WHERE fte.firebaseToken = ?1 and fte.deviceType=?3 AND fte.username = ?2")
    FirebaseTokenEntity findByDeviceIdAndUsername(String token, String s, String deviceType);

    FirebaseTokenEntity findTopByUsernameOrderByLastUpdatedDateDesc(String username);

    @Transactional
    @Modifying
    @Query("UPDATE FirebaseTokenEntity fte SET fte.firebaseToken = ?1, fte.lastUpdatedBy = ?2, fte.lastUpdatedDate = ?3 WHERE fte.username = ?2 AND fte.deviceType = ?4")
    void updateExistingToken(String token, String s, LocalDateTime now, String deviceType);

    @Query("SELECT fte.firebaseToken FROM FirebaseTokenEntity fte WHERE fte.username = ?1")
    List<String> findAllTokenByUsername(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM FirebaseTokenEntity fte WHERE fte.firebaseToken = ?1")
    void deleteByFirebaseToken(String deadToken);
}
