package com.management.shop.repository;

import com.management.shop.entity.NotificationSetting;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSetting, Integer> {

    @Query(value="select * from notification_setting  n where n.username=?1", nativeQuery = true)
    NotificationSetting findbyUsername(String s);

    @Transactional
    @Modifying
    @Query(value = "update notification_setting set payment_reminders=?2, low_stock_alert=?3, system_updates=?4, updated_date = ?5, updated_by=?1 where username=?1", nativeQuery = true)
    void updateNoficationSettings(String s, Boolean paymentReminders, Boolean lowStockAlert, Boolean systemUpdates, LocalDateTime updatedDate);
}
