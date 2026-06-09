package com.topnivo.backend.model.request;

import com.topnivo.backend.model.constant.MemberType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private MemberType memberType;
    private String address;
    private String phoneNumber;
    private String reference;
    private String trxref;
    private String transaction;

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
