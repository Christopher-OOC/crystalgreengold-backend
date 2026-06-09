package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "store_products")
public class StoreProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.EAGER)
    private Product product;
    private int boughtQuantity;
    @ManyToOne(fetch = FetchType.EAGER)
    private Member store;
    @ManyToOne(fetch = FetchType.EAGER)
    private Member boughtFromStore;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StoreProduct that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
