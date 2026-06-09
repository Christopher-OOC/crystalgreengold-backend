package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Cart;
import com.topnivo.backend.model.entity.CartItem;
import com.topnivo.backend.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    CartItem findById(int id);

    List<CartItem> findByCartId(int id);

    void deleteAllByCartId(int id);

    CartItem findByProduct(Product product);

}
