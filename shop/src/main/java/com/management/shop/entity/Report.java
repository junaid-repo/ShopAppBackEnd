package com.management.shop.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "reports_details")
@Data
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    @JsonProperty(value="reportType")
    private String name;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate toDate;

    @Column(nullable = false)
    @JsonProperty(value="generatedAt")
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private String fileName;
    
    //@Column(nullable = false)
    private String status;
    private String userId;
    public Report() {
    }

    
}
