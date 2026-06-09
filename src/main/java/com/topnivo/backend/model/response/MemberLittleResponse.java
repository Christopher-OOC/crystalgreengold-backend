package com.topnivo.backend.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLittleResponse {
    private String memberId;
    private String firstName;
    private String lastName;
    private String username;

}
