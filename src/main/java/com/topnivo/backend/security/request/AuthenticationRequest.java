package com.topnivo.backend.security.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenticationRequest   {

    private String username;
    private String password;

}
