package com.management.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "firebase_notification_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseNotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String eventType;
    private String username;
    private String title;
    @Column(length = 1000)
    private String message;
    private Boolean sentSuccessfully;
    @Column(length = 2000)
    private String response;
    private LocalDateTime sentAt;
}
