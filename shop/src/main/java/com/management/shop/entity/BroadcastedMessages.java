package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="bcm")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BroadcastedMessages {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String username;
    private String topic;
    private String message;
    private String status;
    private LocalDateTime createdDate;
    private String eventCode;
}
