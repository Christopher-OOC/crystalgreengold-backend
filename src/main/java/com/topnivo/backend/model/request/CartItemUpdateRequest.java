package com.topnivo.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemUpdateRequest {
    private int cartItemId;
    private int quantity;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CartItemUpdateRequest that = (CartItemUpdateRequest) o;
        return cartItemId == that.cartItemId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cartItemId);
    }
}
