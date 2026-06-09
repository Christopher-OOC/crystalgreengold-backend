package com.topnivo.backend.model.entity;

import com.topnivo.backend.model.constant.OrderStatus;
import com.topnivo.backend.model.constant.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String orderId;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
    @ManyToOne(fetch = FetchType.LAZY)
    private Member store;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    private String address;
    private String phoneNumber;
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime orderDate;
    @Column(columnDefinition = "DECIMAL(19,2)")
    private double totalAmount;
    @Column(columnDefinition = "TEXT")
    private String pdfText;
    @OneToOne(fetch = FetchType.EAGER)
    private Transaction transaction;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id == order.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", orderDate=" + orderDate +
                ", totalPrice=" + totalAmount +
                '}';
    }
}
