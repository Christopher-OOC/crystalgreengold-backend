package com.topnivo.backend.mapper;

import com.topnivo.backend.model.entity.Product;
import com.topnivo.backend.model.request.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductMapper {

    public Product requestToProduct(ProductCreateRequest request) {
        return Product
                .builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .availableQuantity(request.getAvailableQuantity())
                .bv(request.getBv())
                .pv(request.getPv())
                .build();
    }
}
