package com.topnivo.backend.model.response;

import com.topnivo.backend.model.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarnedPromotionResponse {
    private int id;
    private String name;
    private String description;
    private String image;
    private double targetPv;
    private double prize;
    private MemberResponse member;
    private boolean hasReceived;
    private LocalDateTime dateEarned;
}
