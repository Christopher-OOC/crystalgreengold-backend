package com.topnivo.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCreateRequest {
    private String name;
    private String description;
    private double targetPv;
    private double prize;
    private boolean enabled;
}
