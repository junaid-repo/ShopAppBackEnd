package com.management.shop.repository;

import com.management.shop.entity.ReportsRecordEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRecodsRepository extends JpaRepository<ReportsRecordEntity, Integer> {
    List<ReportsRecordEntity> findByUsername(String s);

    @Transactional
    @Modifying
    @Query(value="update reports_record_entity rre set rre.updated_by=?2, rre.updated_date=?3, rre.is_active=?4 where rre.username=?1", nativeQuery = true)
    void updateReportRecord(String username, String updatedBy, LocalDateTime updatedDate, boolean dailySalesReport);


    @Query(value="select * from reports_record_entity rre where rre.is_active=?1 and rre.is_sent=false limit 50", nativeQuery = true)
    List<ReportsRecordEntity> findAllByStatus(Boolean aTrue);

    @Transactional
    @Modifying
    @Query(value="update reports_record_entity rre set rre.updated_by=?2, rre.updated_date=?3, rre.is_sent=?4 where rre.username=?1", nativeQuery = true)
    void updateReportRecordAfterSending(String username, String updatedBy, LocalDateTime updatedDate, Boolean isSent);

    @Transactional
    @Modifying
    @Query(value="update reports_record_entity rre set rre.updated_by=?1, rre.updated_date=?2, rre.is_sent=?3", nativeQuery = true)
    void updateAllRecordAfterCompletion(String updatedBy, LocalDateTime updatedDate, Boolean isSent);

}
