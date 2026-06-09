package com.topnivo.backend.model.response;

import com.topnivo.backend.model.constant.OrderStatus;
import com.topnivo.backend.model.constant.OrderType;
import com.topnivo.backend.model.entity.Transaction;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String orderId;
    private List<OrderItemResponse> orderItems = new ArrayList<>();
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private OrderType orderType;
    private String address;
    private String phoneNumber;
    private double totalAmount;
    private String pdfText;
    private Transaction transaction;

}
