package com.topnivo.backend.service;


import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.entity.CartItem;
import com.topnivo.backend.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartItemService {

    private final CartItemRepository cartItemRepository;

    public CartItem findById(int id) {
        CartItem cartItem = cartItemRepository.findById(id);
        if (Objects.isNull(cartItem)) {
            throw new NoSuchResourceException(ErrorMessages.NO_CART_ITEM);
        }

        return cartItem;
    }

}
