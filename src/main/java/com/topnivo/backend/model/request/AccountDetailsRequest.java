package com.topnivo.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDetailsRequest {
    private String accountName;
    private String accountNumber;
    private String bankName;
    private String bankCode;
    private String bankType;
    private String currency;
}
