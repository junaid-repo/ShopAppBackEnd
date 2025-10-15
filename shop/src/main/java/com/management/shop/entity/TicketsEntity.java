package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="support_tickets")
public class TicketsEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    private String ticketNumber;
    private LocalDateTime createdDate;
    private String topic;
    private String summary;
    private String status;
    private String closingRemarks;
    private String username;
    private LocalDateTime updatedDate;
    private String updatedBy;


}
