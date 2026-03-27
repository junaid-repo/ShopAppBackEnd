package com.management.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SchedulerRecordEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    private String schedulerName;
    private String cronExpression;

    private LocalDateTime startDateTime ;
    private Boolean isCompleted;
    private LocalDateTime endDateTime;
    private Long durationInSeconds;

    private String updatedBy;
    private LocalDateTime updatedDate;
}
