package com.management.shop.entity;

import jakarta.persistence.*;
import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "global_search_index")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GlobalSearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Boolean sourceIsActive;
    private String sourceId;
    private String sourceType; // "CUSTOMER", "PRODUCT", etc.
    private String searchText;
    private String displayName;
    private String relativeUrl;
    // getters and setters...
}