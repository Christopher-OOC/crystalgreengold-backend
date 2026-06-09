package com.topnivo.backend.model.response;

import com.topnivo.backend.model.constant.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecordResponse {
    private int id;
    private MemberResponse member;
    private String reason;
    private String reference;
    private double amount;
    private TransferStatus status;
}
