package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

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
	@Query(value = "UPDATE shop_customer  SET total_spent = total_spent + ?2 WHERE id = ?1 and user_id = ?3", nativeQuery = true)
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
            value = "SELECT * FROM shop_customer p WHERE p.user_id = :username AND status = 'ACTIVE' AND " +
                    "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true
    )
    Page<CustomerEntity> findAllCustomersWithPagination(
            @Param("username") String username,
            @Param("search") String search,
            Pageable pageable
    );
}
