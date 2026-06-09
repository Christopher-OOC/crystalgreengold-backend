package com.topnivo.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "package_order_items")
public class PackageOrderItem extends OrderItem {
    @ElementCollection
    @CollectionTable(name = "order_package_item_list", joinColumns = @JoinColumn(name = "order_package_id"))
    @Column(name = "package_item_content")
    private List<String> packageItems = new ArrayList<>();
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double directCommissionRate;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double binaryCommissionRate;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double dailyCapping;
}
