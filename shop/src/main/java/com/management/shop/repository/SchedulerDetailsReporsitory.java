package com.management.shop.repository;

import com.management.shop.entity.SchedulerRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerDetailsReporsitory extends JpaRepository<SchedulerRecordEntity, Integer> {
}
