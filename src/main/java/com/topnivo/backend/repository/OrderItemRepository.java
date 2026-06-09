package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.ProductOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<ProductOrderItem, Integer> {
}
