package com.topnivo.backend.model.entity;

import com.topnivo.backend.model.constant.CommissionType;
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
@Table(name = "earning_commission")
public class EarningCommission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double amount;
    private String description;
    private String memberId;
    @Enumerated(EnumType.STRING)
    private CommissionType commissionType;
    private LocalDateTime earnedDate;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EarningCommission that = (EarningCommission) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "EarningCommission{" +
                "id=" + id +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", memberId='" + memberId + '\'' +
                ", earnedDate=" + earnedDate +
                '}';
    }
}
