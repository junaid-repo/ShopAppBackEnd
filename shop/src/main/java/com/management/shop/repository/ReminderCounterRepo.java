package com.management.shop.repository;

import com.management.shop.entity.ReminderCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReminderCounterRepo extends JpaRepository<ReminderCounter, Integer> {

    @Query("select distinct d from ReminderCounter d where d.invoiceId = :invoiceId and d.username = :s order by d.createdDate desc")
    List<ReminderCounter> findByInvoiceIdAndUsername(String invoiceId, String s);
}
