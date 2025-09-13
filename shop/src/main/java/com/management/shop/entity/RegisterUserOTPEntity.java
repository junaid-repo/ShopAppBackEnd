package com.management.shop.entity;

import java.time.LocalDateTime;

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
@Table(name = "NewUO")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterUserOTPEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	private String otp;
	private LocalDateTime createdDate;
	private String username;
	private String status;
	private Integer retries;
    private String userId;
}
