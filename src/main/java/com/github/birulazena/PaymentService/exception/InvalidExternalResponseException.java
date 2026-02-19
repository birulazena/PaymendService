package com.github.birulazena.PaymentService.exception;

public class InvalidExternalResponseException extends RuntimeException {
    public InvalidExternalResponseException(String message) {
        super(message);
    }
}
