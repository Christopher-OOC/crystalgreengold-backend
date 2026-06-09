package com.topnivo.backend.mapper;

import com.topnivo.backend.model.entity.Cart;
import com.topnivo.backend.model.entity.CartItem;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.response.CartItemResponse;
import com.topnivo.backend.model.response.CartResponse;
import com.topnivo.backend.model.response.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartMapper {

    private final ModelMapper modelMapper;
    private final StoreMapper storeMapper;


    private StoreResponse createStoreResponse(Member store) {
        StoreResponse storeResponse = new StoreResponse();

        if (store == null) {
            storeResponse.setAddress("12, Olowu Street, Ikeja, Lagos.");
            storeResponse.setImage(null);
            storeResponse.setPhoneNumber("09087667565");
            storeResponse.setStoreId(null);
            storeResponse.setBusinessName("Topnivo");
            storeResponse.setAccountName("Topnivo Business");
            storeResponse.setBankName("UBA");
            storeResponse.setAccountNumber("2187686878");
        }
        else {
            storeResponse.setAddress(store.getAddress());
            storeResponse.setImage(null);
            storeResponse.setPhoneNumber(store.getPhoneNumber());
            storeResponse.setStoreId(store.getMemberId());
            storeResponse.setBusinessName(store.getBusinessName());

            if (store.getAccountDetails() != null) {
                storeResponse.setAccountName(store.getAccountDetails().getAccountName());
                storeResponse.setBankName(store.getAccountDetails().getBankName());
                storeResponse.setAccountNumber(store.getAccountDetails().getAccountNumber());
            }
        }

        return storeResponse;
    }

    public CartResponse cartToCartResponse(Cart cart) {
        if (Objects.isNull(cart)) {
            return null;
        }

        List<CartItemResponse> cartItemResponseList = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            CartItemResponse cartItemResponse = modelMapper.map(cartItem, CartItemResponse.class);

            Member store = cartItem.getStore();
            StoreResponse storeResponse = createStoreResponse(store);
            cartItemResponse.setStoreResponse(storeResponse);

            cartItemResponseList.add(cartItemResponse);
        }

        var cartResponse = new CartResponse();
        cartResponse.setCartItems(cartItemResponseList);

        return cartResponse;
    }
}
