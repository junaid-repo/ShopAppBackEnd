package com.management.shop.repository;

import com.management.shop.entity.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvoiceSequence i WHERE i.shopId = :shopId AND i.financialYear = :financialYear")
    Optional<InvoiceSequence> findAndLockByShopIdAndFinancialYear(
            @Param("shopId") String shopId,
            @Param("financialYear") String financialYear
    );
}
