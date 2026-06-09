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
@Table(name = "ranks")
public class Rank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double prize;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double qualifyingBv;
    private int rankValue;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rank rank)) return false;
        return id == rank.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
