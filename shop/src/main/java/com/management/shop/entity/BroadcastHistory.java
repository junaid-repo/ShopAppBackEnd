package com.management.shop.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="BroadcastHistory")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BroadcastHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String username;
    private String topic;
    private String message;
    private LocalDateTime createdDate;
}
