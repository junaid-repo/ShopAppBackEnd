package com.management.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupportTicketResponse {


    private String ticketNumber;
    private LocalDateTime createdDate;
    private String topic;
    private String summary;
    private String status;
    private String closingRemarks;
    private String createdBy;


}
