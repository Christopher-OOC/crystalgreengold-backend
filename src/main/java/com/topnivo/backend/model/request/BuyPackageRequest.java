package com.topnivo.backend.model.request;

import lombok.Data;

@Data
public class BuyPackageRequest {
    private int packageId;
    private String storeId;
    private int quantity;
    private String txnReference;

}
