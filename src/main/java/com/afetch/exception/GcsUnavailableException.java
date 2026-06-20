package com.afetch.exception;

public class GcsUnavailableException extends RuntimeException {
    public GcsUnavailableException(String message) {
        super(message);
    }

    public GcsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
