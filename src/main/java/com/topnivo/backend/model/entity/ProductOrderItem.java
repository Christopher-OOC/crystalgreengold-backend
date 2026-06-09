package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_order_items")
public class ProductOrderItem extends OrderItem {

    private String categoryName;
    private String sellerId;

}
