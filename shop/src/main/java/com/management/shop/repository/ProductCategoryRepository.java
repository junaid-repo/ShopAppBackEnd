package com.management.shop.repository;

import com.management.shop.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {

    @Query("SELECT pc FROM ProductCategory pc WHERE LOWER(REPLACE(pc.categoryName, ' ', '')) = LOWER(REPLACE(:categoryName, ' ', '')) AND pc.username = :s")
    List<ProductCategory> getCategoryName(@Param("categoryName") String categoryName, @Param("s") String s);
}
