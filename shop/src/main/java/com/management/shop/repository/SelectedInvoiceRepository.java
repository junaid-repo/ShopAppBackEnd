package com.management.shop.repository;

import com.management.shop.entity.SelectedInvoiceEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface SelectedInvoiceRepository extends JpaRepository<SelectedInvoiceEntity, Integer> {
    SelectedInvoiceEntity findByUsername(String s);

    @Modifying
    @Transactional
    @Query(value="update selected_invoice_entity  set template_name=?1, updated_date=?3 where username=?2 ", nativeQuery = true)
    void updateSelectedInvoice(String selectedTemplate, String s, LocalDateTime now);


}
