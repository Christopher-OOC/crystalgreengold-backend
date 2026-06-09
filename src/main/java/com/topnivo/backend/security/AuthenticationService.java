package com.topnivo.backend.security;

import com.topnivo.backend.security.request.AuthenticationRequest;
import com.topnivo.backend.security.request.RefreshRequest;
import com.topnivo.backend.security.response.AuthenticationResponse;

public interface AuthenticationService {

    AuthenticationResponse login(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshRequest request);


}
