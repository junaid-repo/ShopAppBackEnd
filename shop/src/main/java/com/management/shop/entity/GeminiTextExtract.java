package com.management.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="Gemini_api_log")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GeminiTextExtract {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String url;
    private String request;
    private String response;
    private String status;
    private String name;
    private String attachment;

    private String username;
    private LocalDateTime createdDate;
}
