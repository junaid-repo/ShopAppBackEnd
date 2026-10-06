package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BrodcastNotificationsRequest {
    List<String> usernames;
    String msg;
    String title;
    String username;
    Boolean userFlag;

}
