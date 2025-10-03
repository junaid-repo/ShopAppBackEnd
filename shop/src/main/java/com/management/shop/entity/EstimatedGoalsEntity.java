package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="estimated_goals")
public class EstimatedGoalsEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private Double sales;
    private Double stocks;
    private Double profit;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String userId;
    private String createdBy;
    private String updatedBy;
}
