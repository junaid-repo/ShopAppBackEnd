package com.management.shop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InternalSubscriptionRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String planType;

    @NotBlank
    private String amount;

    @NotBlank
    private String type;
}
