package com.management.shop.repository;

import com.management.shop.entity.GlobalSearchIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GlobalSearchIndexRepository extends JpaRepository<GlobalSearchIndex, Integer> {

    @Query(value = "SELECT * FROM global_search_index " +
            "WHERE user_id = ?1 " +
            "AND source_isactive = ?3 " +
            "AND search_text LIKE CONCAT(LOWER(?2), '%') limit 7", // <-- Changed: No first '%'
            nativeQuery = true)
    List<GlobalSearchIndex> findActiveEntities(String s, String globalSearchTerms, Boolean aTrue);
}
