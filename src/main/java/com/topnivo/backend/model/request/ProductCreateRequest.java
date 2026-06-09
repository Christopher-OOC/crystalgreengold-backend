package com.topnivo.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {
    private String name;
    private String description;
    private double bv;
    private double pv;
    private double price;
    private int availableQuantity;
    private int categoryId;
    private double discount;
    private int packageId;
    private String productType;
}
