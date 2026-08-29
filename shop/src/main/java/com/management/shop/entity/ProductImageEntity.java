package com.management.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
        name = "shop_product_image",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "user_id"})
)
public class ProductImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Lob
    @Column(name = "image_data", nullable = false, columnDefinition = "BLOB")
    private byte[] imageData;
}
