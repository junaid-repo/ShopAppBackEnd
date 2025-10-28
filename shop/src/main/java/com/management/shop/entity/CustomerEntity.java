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
@Table(name="shop_customer")
public class CustomerEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	private String name;
	private String email;
    @Column(name="phone", unique=true,nullable=true)
	private String phone;
	private Double totalSpent;
	private String status;
    private Boolean isActive;
    private String state;
    private String city;
    private String gstNumber;
	private LocalDateTime createdDate;
    private String userId;
    private LocalDateTime updatedDate;
}
