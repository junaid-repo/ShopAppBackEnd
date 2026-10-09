package com.management.shop.gobalusers.entity;

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
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;


    private String username;
    private String ipAddress;
    private String createdBy;
    private Boolean loggedInStatus;
    private LocalDateTime loggedTiming;
    private Long loginCount;
}
