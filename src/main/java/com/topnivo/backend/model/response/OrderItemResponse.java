package com.topnivo.backend.model.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private int id;
    private String name;
    private String description;
    private String image;
    private int bv;
    private int pv;
    private double price;
    private int quantity;
    private int remainingOrderQuantity;
    private String categoryName;
    private StoreResponse store;
    private List<String> packageItems = new ArrayList<>();
    private double directCommissionRate;
    private double binaryCommissionRate;
    private double dailyCapping;
}
