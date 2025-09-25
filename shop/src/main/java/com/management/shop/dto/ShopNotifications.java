package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopNotifications {

    public String id;
    public String title;
    public String message;
    public String subject;
    public String domain;
    public String searchKey;
    public Boolean isFlagged;
    public LocalDateTime createdAt;
    public boolean seen;
}
