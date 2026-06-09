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
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String image;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double bv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double pv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double price;
    private int availableQuantity;
    @ManyToOne
    private Category category;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eligible_package_id")
    private Package eligiblePackage;
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2)")
    private double discount;
    @Column(nullable = true)
    private boolean isDiscounted;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", bv=" + bv +
                ", pv=" + pv +
                ", price=" + price +
                ", availableQuantity=" + availableQuantity +
                ", category=" + category +
                ", package=" + eligiblePackage +
                '}';
    }
}
