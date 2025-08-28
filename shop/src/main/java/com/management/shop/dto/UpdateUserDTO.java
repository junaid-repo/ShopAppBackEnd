package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDTO {


 
    private String username;

    private String name;
    private String email;
    private String phone;
    private String address;
    private String shopOwner;
    private String shopLocation;
    private String gstNumber;
	/*
	 * 
	 * private byte[] profilePic;
	 */

}
