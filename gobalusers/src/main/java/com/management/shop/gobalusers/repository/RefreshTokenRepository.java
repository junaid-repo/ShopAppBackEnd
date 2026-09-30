package com.management.shop.gobalusers.repository;

import com.management.shop.gobalusers.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.username = :username")
    void revokeAllByUsername(@Param("username") String username);

    @Modifying
    @Transactional
    void deleteByUsername(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR r.expiryDate < :now")
    int deleteRevokedAndExpiredTokens(@Param("now") java.time.Instant now);
}
