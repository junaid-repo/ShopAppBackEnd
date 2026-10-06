package com.management.shop.repository;

import com.management.shop.entity.BroadcastedMessages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastedMessageRepository extends JpaRepository<BroadcastedMessages, Long> {
}
