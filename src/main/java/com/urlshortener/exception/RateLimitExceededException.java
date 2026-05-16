package com.urlshortener.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String ipAddress;
    private final long retryAfterMillis;

    public RateLimitExceededException(String message, String ipAddress, long retryAfterMillis) {
        super(message);
        this.ipAddress = ipAddress;
        this.retryAfterMillis = retryAfterMillis;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getRetryAfterSeconds() {
        return retryAfterMillis / 1000;
    }
}