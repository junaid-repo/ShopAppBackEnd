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
@Table(name="ShopDetails_Entity")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopDetailsEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	private String username;
	private String name;
	private String ownerName;
	private String addresss;
	private String gstNumber;
    private String userId;
    private String shopName;
    private String shopEmail;
    private String shopPhone;
}
