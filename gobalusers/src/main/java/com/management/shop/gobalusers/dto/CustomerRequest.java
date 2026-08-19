package com.management.shop.gobalusers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerRequest {
private String id;
private String name;
private String email;
private String phone;
private String gstNumber;
private String customerState ;
private String city;
private String username;
private Long totalSpent;
}
