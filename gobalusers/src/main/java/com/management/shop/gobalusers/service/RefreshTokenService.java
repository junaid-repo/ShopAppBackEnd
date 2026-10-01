package com.management.shop.gobalusers.service;

import com.management.shop.gobalusers.entity.RefreshToken;
import com.management.shop.gobalusers.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {

    // 90 days validity for refresh token
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 90L;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Create a brand new refresh token for a user.
     * Revokes all previous active tokens so only 1 valid token exists per user.
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        refreshTokenRepository.revokeAllByUsername(username);

        RefreshToken refreshToken = RefreshToken.builder()
                .username(username)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify the token exists, is not revoked, and is not expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired or revoked. Please login again.");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Token Rotation: revoke old token and issue a fresh one
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        return createRefreshToken(oldToken.getUsername());
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllUserTokens(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
    }

    /**
     * Runs every day at 12:00 AM (midnight) IST to permanently delete
     * all revoked and expired refresh tokens from the database.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void purgeRevokedAndExpiredTokens() {
        log.info("Starting scheduled cleanup of revoked and expired refresh tokens....");
        try {
            int deletedCount = refreshTokenRepository.deleteRevokedAndExpiredTokens(Instant.now());
            log.info("Refresh token cleanup completed. Deleted {} stale/revoked records.", deletedCount);
        } catch (Exception e) {
            log.error("Failed to purge revoked refresh tokens: {}", e.getMessage(), e);
        }
    }
}