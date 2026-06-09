package com.topnivo.backend.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberNodeResponse {
    private int id;
    private String memberId;
    private String firstName;
    private String lastName;
    private String username;
    private boolean enabled;
    private String rank;
    private int level;
    private double leftPv;
    private double rightPv;
    private double leftBv;
    private double rightBv;
    private String sponsor;
    private MemberNodeResponse left;
    private MemberNodeResponse right;
}
