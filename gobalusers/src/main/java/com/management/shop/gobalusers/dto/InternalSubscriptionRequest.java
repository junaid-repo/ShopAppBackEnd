package com.management.shop.gobalusers.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalSubscriptionRequest {

    private String username;
    private String planType;
    private String amount;
    private String type;
}
