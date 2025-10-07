package com.management.shop.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="Shop_basic")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopBasicEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private String userId;
    private String shopName;
    private String shopSlogan;
    private String address;
    private String shopEmail;
    private String shopPhone;
    private String shopLogolink;
    private String shopPincode;
    private String shopCity;
    private String shopState;
    private String updatedBy;
    private LocalDateTime updatedAt;

}
