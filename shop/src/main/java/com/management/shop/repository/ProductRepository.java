package com.management.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.ProductEntity;

import jakarta.transaction.Transactional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_product SET stock = stock - ?2, status = CASE    WHEN stock - ?2 <= 0 THEN 'Out of Stock' ELSE status END WHERE id = ?1 AND stock > 0 and user_id = ?3", nativeQuery = true)
	void updateProductStock(Integer id, Integer quantity, String userId);

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_product SET stock = stock + ?2,  status = CASE  WHEN stock + ?2 > 0 THEN 'In Stock' ELSE status END WHERE id = ?1 AND stock > 0", nativeQuery = true)
	void addProductStock(Integer id, Integer quantity);

	/*
	 * @Query(value =
	 * "select * from billing_details where created_date BETWEEN ?1 AND ?2",
	 * nativeQuery = true) List<ProductEntity> findProductsByDateRage(LocalDateTime
	 * fromDate, LocalDateTime toDate);
	 */
	@Query(value = "select * from shop_product where active=?1  and user_id=?2", nativeQuery = true)
	List<ProductEntity> findAllByStatus(Boolean isActive, String userId);

	@Query(value ="SELECT * FROM shop_product WHERE created_date >= ?1 	   AND created_date < ?2", nativeQuery=true)
	List<ProductEntity> findAllCreatedToday( LocalDateTime startOfDay,
			LocalDateTime endOfDay);

	@Modifying
	@Transactional
	@Query(value = "UPDATE shop_product SET active = ?2 where id=?1 and user_id=?3", nativeQuery = true)
	void deActivateProduct(Integer id, Boolean isActive, String userId);
	
	
	@Query(value = "select * from shop_product where active=?1 and user_id=?2", nativeQuery = true)
	List<ProductEntity> findAllActiveProducts(Boolean isActive, String userId);

    @Query(value = "select * from shop_product where id=?1 and user_id=?2", nativeQuery = true)
    ProductEntity findByIdAndUserId(Integer id, String userId);


    /**
     * Finds all active products for a given user with optional search and pagination.
     * The search term is matched against the product's name and category.
     *
     * @param isActive The active status of the product (should be true).
     * @param username The username of the owner.
     * @param search   The search term (can be null or empty).
     * @param pageable The pagination and sorting information.
     * @return A Page of ProductEntity matching the criteria.
     */
    @Query(
            value = "SELECT * FROM shop_product p WHERE p.active = :isActive AND p.user_id = :username AND" +
                    "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true
    )
    Page<ProductEntity> findAllActiveProductsWithPagination(
            @Param("isActive") Boolean isActive,
            @Param("username") String username,
            @Param("search") String search,
            Pageable pageable
    );
    @Query(
            value = "SELECT * FROM shop_product p WHERE p.active = :isActive AND p.user_id = :username AND stock > 0 AND" +
                    "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true
    )
    Page<ProductEntity> findAllActiveProductsWithPaginationForBilling(
            @Param("isActive") Boolean isActive,
            @Param("username") String username,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value="SELECT * FROM shop_product p WHERE p.stock <= ?1 AND p.user_id = ?2 AND p.active=?3", nativeQuery = true)
    List<ProductEntity> findByStock(int stock, String username, Boolean isActive);

    @Query(value = "select * from shop_product where active=?1 and user_id=?2", nativeQuery = true)
    List<ProductEntity> getAllProductForReport(Boolean isActive, String userId);

    @Query(
            value = "SELECT * FROM shop_product p WHERE p.active = :isActive AND p.user_id = :username AND stock > 0 AND" +
                    "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%'))) LIMIT :limit",
            nativeQuery = true
    )
    List<ProductEntity> findAllActiveProductsForGSTBilling(
            @Param("isActive") Boolean isActive,
            @Param("username") String username,
            @Param("search") String search,
            @Param("limit") int count
    );

    @Query(value = "SELECT * FROM shop_product WHERE stock < 3 AND user_id = ?1 ORDER BY stock ASC", nativeQuery = true)
    List<ProductEntity> findLowStockProducts(String userId);
}
