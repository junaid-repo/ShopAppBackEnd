package com.management.shop.repository;

import com.management.shop.entity.InvoiceDownloadTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceDownloadTokenRepository extends JpaRepository<InvoiceDownloadTokenEntity, Long> {
    Optional<InvoiceDownloadTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);
}
