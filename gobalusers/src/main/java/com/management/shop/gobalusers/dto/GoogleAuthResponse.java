package com.management.shop.gobalusers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthResponse {
    private boolean success;
    private String token;
    private String message;
    private String username;
    private String secureToken;

    // Keep this custom constructor in case your older code still relies on it!
    public GoogleAuthResponse(boolean success, String token, String message) {
        this.success = success;
        this.token = token;
        this.message = message;
    }
}