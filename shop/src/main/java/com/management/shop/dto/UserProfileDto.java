package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileDto {

    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    private List<String> roles;
}
