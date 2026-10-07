package com.management.shop.gobalusers.repository;

import com.management.shop.gobalusers.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Integer> {
}
