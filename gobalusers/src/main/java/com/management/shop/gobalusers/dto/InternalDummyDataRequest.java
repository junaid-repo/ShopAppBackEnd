package com.management.shop.gobalusers.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InternalDummyDataRequest {
    List<ProductRequest> dummyProducts;
    List<CustomerRequest> dummyCustomers;
}
