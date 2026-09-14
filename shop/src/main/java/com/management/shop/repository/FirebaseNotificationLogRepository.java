package com.management.shop.repository;

import com.management.shop.entity.FirebaseNotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface FirebaseNotificationLogRepository
        extends JpaRepository<FirebaseNotificationLogEntity, Integer> {

    @Modifying
    @Transactional
    @Query("DELETE FROM FirebaseNotificationLogEntity log WHERE log.sentAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
