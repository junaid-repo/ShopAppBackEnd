package com.management.shop.dto;

import lombok.Data;


@Data
public class OtpVerifyRequest {

	private String username;
	private String otp;
}
