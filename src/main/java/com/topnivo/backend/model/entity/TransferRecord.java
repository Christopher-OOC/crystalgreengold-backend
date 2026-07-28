package com.topnivo.backend.model.entity;

import com.topnivo.backend.model.constant.TransferStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transfer_records")
public class TransferRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.EAGER)
    private Member member;
    @Column(unique = true)
    private String reference;
    private String reason;
    private double amount;
    private String recipientCode;
    @Enumerated(EnumType.STRING)
    private TransferStatus status;
    private String transactionId;
}
