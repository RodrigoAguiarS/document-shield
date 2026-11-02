package br.com.documentshield.exception;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String m) {
        super(m);
    }

    public InvalidDocumentException(String m, Throwable c) {
        super(m, c);
    }
}
