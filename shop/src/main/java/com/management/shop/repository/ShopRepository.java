package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.management.shop.dto.CustomerOutstandingDto;
import com.management.shop.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.management.shop.entity.CustomerEntity;

import jakarta.transaction.Transactional;

@Repository
public interface ShopRepository extends JpaRepository<CustomerEntity, Integer> {

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_customer  SET total_spent = total_spent + ?2, updated_date = NOW() WHERE id = ?1 and user_id = ?3", nativeQuery = true)
	void updateCustomerSpentAmount(Integer id, Double spent_value, String userId);

	@Query(value = "SELECT   * FROM    shop_customer WHERE    created_date BETWEEN ?1 AND ?2  and user_id = ?3 ", nativeQuery = true)
	List<CustomerEntity> findCustomerByDateRange(LocalDateTime fromDate, LocalDateTime toDate, String userId);

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_customer  SET status = ?2, updated_date = NOW(), is_active=?4 WHERE id = ?1  and user_id = ?3", nativeQuery = true)
	void updateStatus(Integer id, String status, String userId, Boolean isActive);

	@Query(value = "SELECT   * FROM    shop_customer WHERE    status=?1 and user_id=?2 order by total_spent desc", nativeQuery = true)
	List<CustomerEntity> findAllActiveCustomer(String status, String userId);

    @Query(value = "SELECT DATE_FORMAT(sc.created_date, '%b') AS month, " +
            "COUNT(sc.id) AS customerCount " +
            "FROM shop_customer sc " +
            "WHERE sc.created_date BETWEEN :fromDate AND :toDate " +
            "AND sc.user_id = :userId " +
            "GROUP BY MONTH(sc.created_date), DATE_FORMAT(sc.created_date, '%b') " +
            "ORDER BY MONTH(sc.created_date)",
            nativeQuery = true)
    List<Object[]> getMonthlyCustomerCount(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("userId") String userId
    );

    @Query(value = "SELECT * FROM shop_customer WHERE phone = ?1 AND status = ?2 and user_id=?3" , nativeQuery = true)
    List<CustomerEntity> findByPhone(String phone, String aTrue, String userId);

    @Query(value = "SELECT * FROM shop_customer WHERE id = ?1  and user_id=?2" , nativeQuery = true)
    CustomerEntity findByIdAndUserId(Integer id, String userId);

    @Query(
            value = "SELECT * FROM shop_customer p WHERE p.user_id = :username AND status = 'ACTIVE' AND  is_active = :is_active AND" +
                    "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true
    )
    Page<CustomerEntity> findAllCustomersWithPagination(
            @Param("username") String username,
            @Param("search") String search,
            Pageable pageable,
            @Param("is_active") Boolean is_active
    );

    @Query(value = "SELECT " +
            "CASE " +
            "WHEN sc.gst_number IS NULL OR sc.gst_number = '' THEN 'Without GST' " +
            "ELSE 'With GST' " +
            "END AS gst_status, " +
            "COUNT(sc.id) AS customerCount " +
            "FROM shop_customer sc " +
            "WHERE sc.user_id = :userId " +
            "AND sc.updated_date IS NOT NULL " +
            "AND sc.updated_date BETWEEN :fromDate AND :toDate " +
            "GROUP BY gst_status",
            nativeQuery = true)
    List<Object[]> getCustomerGstSummary(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("userId") String userId
    );
    @Query(value = "SELECT " +
            "    c.name as name, " +
            "    c.phone as phone, " +
            "    c.email as email, " +
            "    SUM(b.remaining_amount) as totalOutstanding, " +
            "    GROUP_CONCAT(b.invoice_number SEPARATOR ', ') as invoiceList " +
            "FROM " +
            "    shop_customer c " +
            "JOIN " +
            "    billing_details b ON c.id = b.customer_id AND c.user_id = b.user_id " +
            "WHERE " +
            "    b.user_id = ?1 " +
            "    AND b.remaining_amount > 0 " + // Filter for outstanding bills
            "GROUP BY " +
            "    c.id, c.name, c.phone, c.email " +
            "HAVING " +
            "    totalOutstanding > 0 " +
            "ORDER BY " +
            "    totalOutstanding DESC", nativeQuery = true)
    List<CustomerOutstandingDto> findCustomersWithOutstandingAmount(String userId);
}
