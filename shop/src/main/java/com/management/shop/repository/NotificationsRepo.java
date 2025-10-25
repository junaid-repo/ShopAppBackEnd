package com.management.shop.repository;

import com.management.shop.entity.MessageEntity;
import com.management.shop.scheduler.Notifications;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationsRepo extends JpaRepository<MessageEntity,Integer> {

    @Query (value= "SELECT * FROM shop_message  WHERE user_id = ?1  AND (?2 = 'all' OR domain = ?2) and is_deleted=?3",nativeQuery = true)
    Page<MessageEntity> findAllNotifications(String s, String domain,  Boolean isDeleted, Pageable pageable);

    @Query (value="select * from shop_message where user_id  =?1 AND (?2 = 'all' OR domain = ?2) and is_read =?3 and is_deleted=?4 ",nativeQuery = true)
    Page<MessageEntity> findAllNotificationsByReadStatus(String s, String domain, Boolean isRead,  Boolean isDeleted, Pageable pageable);

    @Query (value="select * from shop_message where user_id  =?1 AND (?2 = 'all' OR domain = ?2) and is_flagged =?3 and is_deleted=?4",nativeQuery = true)
    Page<MessageEntity> findAllNotificationsByFlaggedStatus(String s, String domain, Boolean isRead, Boolean isDeleted, Pageable pageable);


    @Transactional
    @Modifying
    @Query (value="update shop_message set is_read = ?3 where user_id  =?2 and id=?1",nativeQuery = true)
    void updateNotificationStatus(Integer id, String userId,  Boolean seen);

    @Transactional
    @Modifying
    @Query (value="update shop_message set is_flagged = ?3 where user_id  =?2 and id=?1",nativeQuery = true)
    void updateNotificationFlaggedStatus(Integer id, String userId,  Boolean flagged);

    @Transactional
    @Modifying
    @Query (value="update shop_message set is_deleted = ?3 where user_id  =?2 and id=?1",nativeQuery = true)
    void updateNotificationDeleteStatus(Integer id, String userId,  Boolean isDeleted);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM shop_message m WHERE m.is_flagged = false AND m.created_date < (NOW() - INTERVAL '48' MINUTE ) AND m.id NOT IN (SELECT max_id FROM (SELECT MAX(sub.id) as max_id FROM shop_message sub WHERE sub.is_flagged = false AND sub.created_date < (NOW() - INTERVAL '48' DAY) AND sub.title = m.title AND sub.subject = m.subject AND sub.details = m.details AND sub.domain = m.domain GROUP BY sub.title, sub.subject, sub.details, sub.domain) as keep_ids)", nativeQuery = true)
    void deleteOldUnflaggedDuplicates();

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM shop_message m WHERE m.is_deleted = true AND m.created_date < (NOW() - INTERVAL '24' HOUR)", nativeQuery = true)
    void deleteOldDeletedMessages();
}
