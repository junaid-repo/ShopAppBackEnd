package com.management.shop.repository;

import com.management.shop.entity.EmailRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRecordRepo extends JpaRepository<EmailRecord, Integer> {
}
