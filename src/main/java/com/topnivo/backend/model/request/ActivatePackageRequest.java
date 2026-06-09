package com.topnivo.backend.model.request;

import lombok.Data;

@Data
public class ActivatePackageRequest {
    private int packageId;
    private String storeId;
    private String txnReference;

}
