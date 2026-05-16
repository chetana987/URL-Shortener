package com.urlshortener.exception;

public class UrlInactiveException extends RuntimeException {

    public UrlInactiveException(String message) {
        super(message);
    }

    public UrlInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}