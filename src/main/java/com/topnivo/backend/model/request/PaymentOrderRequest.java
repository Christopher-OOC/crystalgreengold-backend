package com.topnivo.backend.model.request;

import com.topnivo.backend.model.constant.MemberType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderRequest {
    private String reference;
    private String trxref;
    private String transaction;
    private MemberType memberType;
    private String address;
    private String phoneNumber;

    public String getPaymentReference() {
        if (reference != null && !reference.isBlank()) {
            return reference;
        }
        if (trxref != null && !trxref.isBlank()) {
            return trxref;
        }
        if (transaction != null && !transaction.isBlank()) {
            return transaction;
        }

        return null;
    }
}
