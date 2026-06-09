package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double prize;
    private boolean enabled;
    @Column(columnDefinition = "TEXT")
    private String image;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double targetPv;
    private LocalDateTime endDate;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Promotion promotion)) return false;
        return Double.compare(targetPv, promotion.targetPv) == 0 && Objects.equals(id, promotion.id) && Objects.equals(name, promotion.name) && Objects.equals(description, promotion.description) && Objects.equals(image, promotion.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, image, targetPv);
    }
}
