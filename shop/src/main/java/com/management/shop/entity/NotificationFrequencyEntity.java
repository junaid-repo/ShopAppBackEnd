package com.management.shop.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationFrequencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String frequencyType; // e.g., "DAILY", "WEEKLY", "MONTHLY"
    private String frequencyCount;
    private String key;

    private String username;
    private String createdDate;
    private String createdBy;
}
