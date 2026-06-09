package com.topnivo.backend.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {
    private String storeId;
    private String businessName;
    private String phoneNumber;
    private String address;
    private String image;
    private String accountName;
    private String accountNumber;
    private String bankName;
}
