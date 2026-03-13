package com.management.shop.gobalusers.dto;

import lombok.ToString;

@ToString
public class GoogleAuthResponse {
    private boolean success;
    private String token;
    private String message;
    private String username;
    private String secureToken;

    public GoogleAuthResponse() {}

    public void setSecureToken(String secureToken) {
        this.secureToken = secureToken;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public GoogleAuthResponse(boolean success, String token, String message) {
        this.success = success;
        this.token = token;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

