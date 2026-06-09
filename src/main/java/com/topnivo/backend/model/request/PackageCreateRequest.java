package com.topnivo.backend.model.request;

import com.topnivo.backend.model.entity.PackageCommissionRate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageCreateRequest {
    private String name;
    private String description;
    private double price;
    private double bv;
    private double pv;
    private List<String> packageItems = new ArrayList<>();
    private double directCommissionRate;
    private double binaryCommissionRate;
    private double dailyCapping;

}
