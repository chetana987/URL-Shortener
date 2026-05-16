package com.urlshortener.util;

import java.time.LocalDateTime;

public class DateUtils {

    public static boolean isExpired(LocalDateTime expirationDate) {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    public static LocalDateTime calculateExpiration(Long days) {
        if (days == null || days <= 0) {
            return null;
        }
        return LocalDateTime.now().plusDays(days);
    }
}
