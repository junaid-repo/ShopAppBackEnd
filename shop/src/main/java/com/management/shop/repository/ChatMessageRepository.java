package com.management.shop.repository;

import com.management.shop.entity.ChatMessageEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByTicketNumberOrderByTimestampAsc(String ticketNumber);

    @Modifying
    @Transactional
    @Query(value="delete  from chat_messages cm where cm.ticket_number=?1", nativeQuery = true)
    void removeClosedChatHistory(String ticketNumber);
}
