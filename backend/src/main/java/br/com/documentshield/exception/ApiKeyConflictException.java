package br.com.documentshield.exception;

public class ApiKeyConflictException extends RuntimeException {
    public ApiKeyConflictException(String message) {
        super(message);
    }
}
