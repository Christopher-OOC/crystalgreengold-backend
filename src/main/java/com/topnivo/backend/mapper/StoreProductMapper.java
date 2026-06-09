package com.topnivo.backend.mapper;

import com.topnivo.backend.model.entity.StoreProduct;
import com.topnivo.backend.model.response.StoreProductResponse;
import com.topnivo.backend.model.response.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreProductMapper {

    private final ModelMapper modelMapper;

    public StoreProductResponse storeProductToResponse(StoreProduct storeProduct) {
        StoreProductResponse response = modelMapper.map(storeProduct, StoreProductResponse.class);
        // Create a store response
        StoreResponse storeResponse = StoreResponse
                .builder()
                .storeId(storeProduct.getStore().getMemberId())
                .image(storeProduct.getStore().getImage())
                .businessName(storeProduct.getStore().getBusinessName())
                .address(storeProduct.getStore().getAddress())
                .phoneNumber(storeProduct.getStore().getPhoneNumber())
                .build();
        response.setProductOwner(storeResponse);

        return response;
    }
}
