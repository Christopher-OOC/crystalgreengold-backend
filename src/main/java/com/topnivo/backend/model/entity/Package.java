package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "packages")
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    @Column(columnDefinition = "TEXT")
    private String image;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double price;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double bv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double pv;
    @ElementCollection
    @CollectionTable(name = "package_item_list", joinColumns = @JoinColumn(name = "package_id"))
    @Column(name = "package_item_content")
    private List<String> packageItems = new ArrayList<>();
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double directCommissionRate;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double binaryCommissionRate;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double dailyCapping;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Package aPackage = (Package) o;
        return id == aPackage.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Package{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", bv=" + bv +
                ", pv=" + pv +
                '}';
    }
}
