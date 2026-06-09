package com.topnivo.backend.model.response;

import com.topnivo.backend.model.constant.TransactionStatus;
import com.topnivo.backend.model.constant.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private int id;
    private String referenceId;
    private String memberId;
    private String orderId;
    private double amount;
    private LocalDateTime transactionDate;
    private String message;
    private TransactionType type;
    private TransactionStatus status;
}
