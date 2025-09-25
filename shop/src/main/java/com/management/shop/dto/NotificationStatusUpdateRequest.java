package com.management.shop.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationStatusUpdateRequest {
    private List<Integer> notificationIds;
    private boolean seen;
}
