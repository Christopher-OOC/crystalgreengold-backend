package com.topnivo.backend.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankCreateRequest {
    private String name;
    private double prize;
    private double qualifyingBv;
    private int rankValue;
}
