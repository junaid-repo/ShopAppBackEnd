package com.management.shop.repository;

import com.management.shop.entity.GeminiTextExtract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiSaveRepository extends JpaRepository<GeminiTextExtract, Integer> {

    @Query("SELECT g FROM GeminiTextExtract g WHERE g.createdDate >= :time and g.username=:username and g.name=:apiName")
    List<GeminiTextExtract> findCreatedWithinLast24Hours(@Param("time") LocalDateTime time, @Param("username") String username, @Param("apiName") String apiName);
}
