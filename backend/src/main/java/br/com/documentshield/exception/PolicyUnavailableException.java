package br.com.documentshield.exception;

public class PolicyUnavailableException extends RuntimeException {
    public PolicyUnavailableException(String message) {
        super(message);
    }
}
