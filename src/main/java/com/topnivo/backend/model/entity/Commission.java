package com.topnivo.backend.model.entity;

import com.topnivo.backend.model.constant.CommissionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "commissions")
public class Commission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    private CommissionType commissionType;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double value; // In percentage (%)

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Commission commission = (Commission) o;
        return id == commission.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Commission{" +
                "id=" + id +
                ", commissionType=" + commissionType +
                ", value=" + value +
                '}';
    }
}
