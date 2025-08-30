package com.management.shop.gobalusers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

}
