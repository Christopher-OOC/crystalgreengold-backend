package com.topnivo.backend.util;

import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.JwtExpiredException;
import com.topnivo.backend.exception.exception.UnknownException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${application.secretKey}")
    private String jwtSecretKet;

    public String generateLoginJwt(String username) {
        byte[] encodedKey = Base64.getEncoder().encode(jwtSecretKet.getBytes());
        SecretKey secretKey = new SecretKeySpec(encodedKey, SignatureAlgorithm.HS512.getJcaName());
        Date now = Date.from(Instant.now());
        Date expiration = Date.from(Instant.now().plusSeconds(10000000));

        return Jwts
                .builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String decodeLoginJwt(String jwt) {
        byte[] encodedKey = Base64.getEncoder().encode(jwtSecretKet.getBytes());
        SecretKey secretKey = new SecretKeySpec(encodedKey, SignatureAlgorithm.HS512.getJcaName());

        String username = null;

        try {
            JwtParser jwtParser = Jwts
                    .parser()
                    .setSigningKey(secretKey)
                    .build();
            Claims payload = jwtParser.parseSignedClaims(jwt).getPayload();
            username = payload.getSubject();
            Date expiration = payload.getExpiration();

            if (expiration.before(new Date())) {
                throw new JwtExpiredException(ErrorMessages.JWT_EXPIRED);
            }
        }
        catch (Exception ex) {
            throw new UnknownException(ErrorMessages.JWT_ERROR);
        }

        return username;
    }
}
