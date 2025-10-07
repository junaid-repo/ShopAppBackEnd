package com.management.shop.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="shop_product")
public class ProductEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	private String name;
	private String category;
	private String status;
	private Integer price;
    private Integer costPrice;
	private Integer stock;
	private LocalDateTime createdDate;
	private Boolean active;
	@JsonProperty("tax")
	private Integer taxPercent;
    private String hsn;
    private String userId;
    private LocalDateTime updatedDate;
    private String updatedBy;
}
