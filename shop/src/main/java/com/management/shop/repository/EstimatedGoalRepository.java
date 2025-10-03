package com.management.shop.repository;

import com.management.shop.entity.EstimatedGoalsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EstimatedGoalRepository extends JpaRepository<EstimatedGoalsEntity, Integer> {

    @Query(value="Select * from estimated_goals where user_id=?1", nativeQuery = true)
    EstimatedGoalsEntity findByUserId(String s);
}
