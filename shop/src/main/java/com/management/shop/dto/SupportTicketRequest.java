package com.management.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupportTicketRequest {

    private String ticketNumber;
    private LocalDateTime createdDate;
    private String topic;
    private String summary;
    private String status;
    private String closingRemarks;
    private String username;
}
