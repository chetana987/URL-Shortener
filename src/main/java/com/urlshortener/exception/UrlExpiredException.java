package com.urlshortener.exception;

public class UrlExpiredException extends RuntimeException {

    private final String shortCode;
    private final String expiredAt;

    public UrlExpiredException(String message) {
        super(message);
        this.shortCode = null;
        this.expiredAt = null;
    }

    public UrlExpiredException(String message, String shortCode, String expiredAt) {
        super(message);
        this.shortCode = shortCode;
        this.expiredAt = expiredAt;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getExpiredAt() {
        return expiredAt;
    }

    public static UrlExpiredException expired(String shortCode, String expiredAt) {
        return new UrlExpiredException(
                "This URL has expired and is no longer accessible",
                shortCode,
                expiredAt
        );
    }
}