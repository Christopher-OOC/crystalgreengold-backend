package com.topnivo.backend.service;

import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.RoleRepository;
import com.topnivo.backend.security.AuthenticationService;
import com.topnivo.backend.security.JwtService;
import com.topnivo.backend.security.request.AuthenticationRequest;
import com.topnivo.backend.security.request.RefreshRequest;
import com.topnivo.backend.security.response.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;

    @Override
    public AuthenticationResponse login(AuthenticationRequest request) {

        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        final User user = (User) authentication.getPrincipal();
        final String token = this.jwtService.generateAccessToken(user.getUsername());
        final String refreshToken = this.jwtService.generateRefreshToken(user.getUsername());
        final String tokenType = "Bearer";

        final Member member = memberRepository.findByUsernameIgnoreCase(user.getUsername());

        return AuthenticationResponse
                .builder()
                .tokenType(tokenType)
                .refreshToken(refreshToken)
                .accessToken(token)
                .memberId(member.getMemberId())
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request) {
        final String newAccessToken = this.jwtService.generateAccessToken(request.getRefreshToken());
        final String tokenType = "Bearer";

        return AuthenticationResponse
                .builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType(tokenType)
                .build();
    }

}
