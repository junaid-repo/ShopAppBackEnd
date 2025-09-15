package com.management.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="ProductSales")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductSalesEntity {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	
	private Integer billingId;
	private Integer productId;
	private Integer quantity;
	private Integer tax;
	private Integer subTotal;
	private Integer total;
    private Double discountPercentage;
    private Double profitOnCP;
    private String userId;

}
