package com.topnivo.backend.controller;

import com.topnivo.backend.mapper.CartMapper;
import com.topnivo.backend.mapper.StoreMapper;
import com.topnivo.backend.model.entity.Cart;
import com.topnivo.backend.model.request.CartItemUpdateRequest;
import com.topnivo.backend.model.request.CartItemCreateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.CartResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartMapper cartMapper;
    private final StoreMapper storeMapper;

    @PostMapping(value = "/add-to-cart/{memberId}")
    public ResponseEntity<?> addProductToCart(
            @RequestBody CartItemCreateRequest request,
            @PathVariable("memberId") String memberId
    ) {
        Cart cart = cartService.addProductToCart(memberId, request);
        CartResponse cartResponse = cartMapper.cartToCartResponse(cart);
        ApiResponse<CartResponse> response = new ApiResponse<>(
                ResponseStatus.ADDED.name(),
                "Product added to your cart successfully!",
                cartResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{memberId}")
    public ResponseEntity<?> findCartByMemberId(
            @PathVariable("memberId") String memberId
    ) {
        Cart cart = cartService.findCartByMemberId(memberId);
        CartResponse cartResponse = cartMapper.cartToCartResponse(cart);
        ApiResponse<CartResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Cart item retrieve successfully!",
                cartResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    // Update Cart OrderItem
    @PutMapping(value = "/{memberId}")
    public ResponseEntity<?> updateCartItemQuantity(
            @RequestBody List<CartItemUpdateRequest> request,
            @PathVariable("memberId") String memberId
    ) {
        Cart cart = cartService.updateCartItemQuantity(memberId, request);
        CartResponse cartResponse = cartMapper.cartToCartResponse(cart);
        ApiResponse<CartResponse> response = new ApiResponse<>(
                ResponseStatus.ADDED.name(),
                "Cart item updated successfully!",
                cartResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/{memberId}")
    public ResponseEntity<?> deleteCartItem(
            @RequestParam("cartItemId") int cartItemId,
            @PathVariable("memberId") String memberId
    ) {
        Cart cart = cartService.deleteCartItemFromCart(memberId, cartItemId);
        CartResponse cartResponse = cartMapper.cartToCartResponse(cart);
        ApiResponse<CartResponse> response = new ApiResponse<>(
                ResponseStatus.DELETED.name(),
                "Cart item deleted successfully!",
                cartResponse,
                null
        );

        return ResponseEntity.ok(response);
    }
}
