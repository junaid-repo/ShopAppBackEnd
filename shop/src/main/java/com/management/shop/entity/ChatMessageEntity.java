package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticketNumber; // Foreign key to your Ticket entity

    @Column(nullable = false)
    private String sender; // e.g., "User_123" or "Admin_Support"

    @Lob // Use @Lob for potentially long message content
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    // Standard getters and setters...
}
