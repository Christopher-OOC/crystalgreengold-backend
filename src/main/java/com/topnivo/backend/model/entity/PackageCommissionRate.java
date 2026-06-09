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
@Table(name = "commission_rates")
public class PackageCommissionRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private double referralCommissionLevel1;
    private double referralCommissionLevel2;
    private double referralCommissionLevel3;
    private double referralCommissionLevel4;
    private double referralCommissionLevel5;
    private double matchingCommissionLevel1;
    private double matchingCommissionLevel2;
    private double matchingCommissionLevel3;
    private double upgradeCommission;
    private double binaryCommission;
    private double unilevelCommission;
    private double dailyCapping;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PackageCommissionRate that = (PackageCommissionRate) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
