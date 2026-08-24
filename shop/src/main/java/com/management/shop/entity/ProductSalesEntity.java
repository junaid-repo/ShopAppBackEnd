package com.management.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double tax;
	@Column(columnDefinition = "DECIMAL(19,2)")
	private Double cgst;
    @Column(columnDefinition = "DECIMAL(7,4)")
    private Double cgstPercentage;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double sgst;
    @Column(columnDefinition = "DECIMAL(7,4)")
    private Double sgstPercentage;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double igst;
    @Column(columnDefinition = "DECIMAL(7,4)")
    private Double igstPercentage;
	@Column(columnDefinition = "DECIMAL(19,2)")
	private Double subTotal;
	@Column(columnDefinition = "DECIMAL(19,2)")
	private Double total;
    private String productDetails;
    @Column(columnDefinition = "DECIMAL(7,4)")
    private Double discountPercentage;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double discountAmount;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private Double profitOnCP;
    private LocalDateTime updatedAt;
    private String userId;

}
