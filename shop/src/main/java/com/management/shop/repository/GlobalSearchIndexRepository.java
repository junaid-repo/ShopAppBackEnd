package com.management.shop.repository;

import com.management.shop.entity.GlobalSearchIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GlobalSearchIndexRepository extends JpaRepository<GlobalSearchIndex, Integer> {

    @Query(value = "SELECT * FROM global_search_index " +
            "WHERE user_id = ?1 " +
            "AND source_isactive = ?3 " +
            "AND LOWER(search_text) LIKE CONCAT('%', LOWER(?2), '%') " + // <-- Changed: Added '%' at the start
            "LIMIT 7",
            nativeQuery = true)
    List<GlobalSearchIndex> findActiveEntities(String userId, String globalSearchTerms, Boolean isActive);
}
