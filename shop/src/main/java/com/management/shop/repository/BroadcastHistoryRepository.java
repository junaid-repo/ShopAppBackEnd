package com.management.shop.repository;

import com.management.shop.entity.BroadcastHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BroadcastHistoryRepository extends JpaRepository<BroadcastHistory, Long> {
    List<BroadcastHistory> findAllByUsername(String s);
}
