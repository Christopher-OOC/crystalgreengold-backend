package com.topnivo.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${application.secretKey}")
    private String secretKey;
    @Value("${application.access-token-expiration}")
    private long accessTokenExpiration;
    @Value("${application.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final String TOKEN_TYPE = "token_type";

    public String generateAccessToken(String username) {
        final Map<String, Object> claims = Map.of(TOKEN_TYPE, "ACCESS_TOKEN");
        return buildToken(username, claims, this.accessTokenExpiration);
    }

    public String generateRefreshToken(String username) {
        final Map<String, Object> claims = Map.of(TOKEN_TYPE, "REFRESH_TOKEN");
        return buildToken(username, claims, this.refreshTokenExpiration);
    }

    private String buildToken(String username, Map<String, Object> claims, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(KeyUtils.getSecretKey(secretKey))
                .compact();
    }

    public boolean isTokenValid(final String token) {
        final String username = extractUsername(token);

        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());

    }

    public String extractUsername(String token) {

        return extractClaims(token).getSubject();
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KeyUtils.getSecretKey(secretKey))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
        catch (final JwtException ex) {
            throw new JwtException("Invalid token");
        }
    }

    public String refreshAccessToken(String refreshToken) throws Exception {
        final Claims claims = extractClaims(refreshToken);
        if (!"REFRESH_TOKEN".equals(claims.get(TOKEN_TYPE))) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh Token is expired");
        }

        final String username = claims.getSubject();
        return generateAccessToken(username);
    }


}
