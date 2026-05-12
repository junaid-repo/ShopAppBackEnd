package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
        name = "shop_product_category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_username",
                        columnNames = {"category_name", "username"}
                )
        }
)
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    // Remove unique=true from here
    @Column(name = "category_name", updatable = false)
    private String categoryName;

    private String type;

    private String username;

    private String updatedBy;

    private LocalDateTime updateDate;
}