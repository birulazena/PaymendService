package com.github.birulazena.PaymentService.exception;

public class ExternalNotFoundException extends RuntimeException {
    public ExternalNotFoundException(String message) {
        super(message);
    }
}
