package com.topnivo.backend.model.response;

import com.topnivo.backend.model.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProductResponse {
    private int id;
    private StoreResponse productOwner;
    private Product product;
    private int boughtQuantity;
}
