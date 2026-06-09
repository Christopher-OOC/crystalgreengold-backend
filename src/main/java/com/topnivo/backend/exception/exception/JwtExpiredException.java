package com.topnivo.backend.exception.exception;

public class JwtExpiredException extends RuntimeException {

    public JwtExpiredException(String message) {
        super(message);
    }
}
