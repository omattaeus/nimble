package com.nimble.gateway.domain.exception;

public class PaymentAuthorizationException extends RuntimeException {
    
    public PaymentAuthorizationException(String message) {
        super(message);
    }
    
    public PaymentAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
