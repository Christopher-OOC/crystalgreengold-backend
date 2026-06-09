package com.topnivo.backend.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEmailResponse {
    private String name;
    private double price;
    private int quantity;
    private double totalPv;
    private double totalBv;
    private double totalPrice;
}
