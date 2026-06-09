package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "earned_promotions")
public class EarnedPromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    @Column(columnDefinition = "TEXT")
    private String image;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double targetPv;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double prize;
    @ManyToOne(fetch = FetchType.EAGER)
    private Member member;
    private boolean hasReceived;
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dateEarned;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EarnedPromotion that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
