package com.topnivo.backend.model.response;

import com.topnivo.backend.model.constant.CommissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningCommissionResponse {
    private int id;
    private double amount;
    private String description;
    private String memberId;
    private CommissionType commissionType;
    private LocalDateTime earnedDate;
}
