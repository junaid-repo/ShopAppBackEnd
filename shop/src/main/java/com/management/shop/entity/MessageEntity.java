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
@Table(name="shop_message")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private String title;
    private String subject;
    private String details;
    private String domain;
    private String searchKey;
    private Boolean isRead;
    private Boolean isDeleted;
    private Boolean isFlagged;
    private Boolean isDone;
    private Boolean getIsDeleted;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String updatedBy;
    private String userId;
}
