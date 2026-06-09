package com.topnivo.backend.model.entity;

import com.topnivo.backend.model.constant.OrderType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Data
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;
    private String originalId;
    private String name;
    private String description;
    @Column(columnDefinition = "TEXT")
    private String image;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double bv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double pv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double price;
    private int quantity;
    private int remainingOrderQuantity;
    @ManyToOne
    private Order order;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return id == orderItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
