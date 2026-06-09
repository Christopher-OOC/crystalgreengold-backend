package com.topnivo.backend.exception.handler;

import com.topnivo.backend.exception.exception.*;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<?> handleInvalidCredentialException(InvalidCredentialException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(NoSuchResourceException.class)
    public ResponseEntity<?> handleNoSuchResourceException(NoSuchResourceException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientProductQuantity.class)
    public ResponseEntity<?> handleInsufficientProductQuantity(InsufficientProductQuantity ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }



    @ExceptionHandler(ResourceAlreadyExistException.class)
    public ResponseEntity<?> handleResourceAlreadyExistException(ResourceAlreadyExistException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequestException(BadRequestException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(PackageDowngradeException.class)
    public ResponseEntity<?> handlePackageDowngradeException(PackageDowngradeException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<?> handleInvalidOrderException(InvalidOrderException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<?> handleInvalidPaymentException(InvalidPaymentException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(UnknownException.class)
    public ResponseEntity<?> handleUnknownException(UnknownException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<?> handleLoginException(LoginException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(403).body(error);
    }

    @ExceptionHandler(JwtExpiredException.class)
    public ResponseEntity<?> handleJwtExpiredException(JwtExpiredException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(401).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ApiResponse<Object> error = new ApiResponse<>(
                ResponseStatus.ERROR.name(),
                ex.getLocalizedMessage(),
                null,
                null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
