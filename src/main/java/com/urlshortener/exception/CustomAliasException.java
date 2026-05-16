package com.urlshortener.exception;

public class CustomAliasException extends RuntimeException {

    private final ErrorCode errorCode;

    public enum ErrorCode {
        ALIAS_ALREADY_EXISTS,
        ALIAS_INVALID_FORMAT,
        ALIAS_RESERVED,
        ALIAS_TOO_SHORT,
        ALIAS_TOO_LONG
    }

    public CustomAliasException(String message) {
        super(message);
        this.errorCode = ErrorCode.ALIAS_ALREADY_EXISTS;
    }

    public CustomAliasException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static CustomAliasException aliasAlreadyExists(String alias) {
        return new CustomAliasException(
                "Custom alias '" + alias + "' is already in use",
                ErrorCode.ALIAS_ALREADY_EXISTS
        );
    }

    public static CustomAliasException aliasInvalidFormat(String alias) {
        return new CustomAliasException(
                "Custom alias '" + alias + "' contains invalid characters. Use only letters, numbers, hyphens and underscores",
                ErrorCode.ALIAS_INVALID_FORMAT
        );
    }

    public static CustomAliasException aliasReserved(String alias) {
        return new CustomAliasException(
                "Custom alias '" + alias + "' is reserved and cannot be used",
                ErrorCode.ALIAS_RESERVED
        );
    }
}
