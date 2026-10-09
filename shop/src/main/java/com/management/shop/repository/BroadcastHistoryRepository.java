package com.management.shop.repository;

import com.management.shop.entity.BroadcastHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BroadcastHistoryRepository extends JpaRepository<BroadcastHistory, Long> {

    @Query(value = "SELECT e.* \n" +
            "FROM broadcast_history e \n" +
            "WHERE e.id IN (\n" +
            "    SELECT MAX(e2.id) \n" +
            "    FROM broadcast_history e2 \n" +
            "  where username = ?1\n" +
            "    GROUP BY e2.topic\n" +
            ")\n", nativeQuery = true)
    List<BroadcastHistory> findAllByUsername(String s);
}
