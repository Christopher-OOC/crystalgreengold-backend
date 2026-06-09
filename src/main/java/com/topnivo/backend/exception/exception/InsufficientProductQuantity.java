package com.topnivo.backend.exception.exception;

public class InsufficientProductQuantity extends RuntimeException {

    public InsufficientProductQuantity(String message) {
        super(message);
    }

}
