package com.urlshortener.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@Component
public class ShortCodeGenerator {

    private static final Pattern VALID_URL_PATTERN = Pattern.compile(
            "^(https?://)?([\\w.-]+)\\.[a-z]{2,}(\\/.*)?$",
            Pattern.CASE_INSENSITIVE
    );

    private final int shortCodeLength;
    private final MessageDigest sha256;

    public ShortCodeGenerator(@Value("${app.url.short-code-length:7}") int shortCodeLength) {
        this.shortCodeLength = shortCodeLength;
        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a random short code using Base62 encoding.
     *
     * @return A random short code of configured length
     */
    public String generate() {
        long randomValue = Math.abs((long) (Math.random() * Long.MAX_VALUE));
        String encoded = Base62Encoder.encode(randomValue);

        if (encoded.length() >= shortCodeLength) {
            return encoded.substring(0, shortCodeLength);
        }

        // Pad with random characters if needed
        StringBuilder sb = new StringBuilder(encoded);
        while (sb.length() < shortCodeLength) {
            long pad = Math.abs((long) (Math.random() * 62));
            sb.append(Base62Encoder.DIGITS[(int) pad]);
        }
        return sb.toString();
    }

    /**
     * Generates a deterministic short code from a URL using SHA-256 + Base62.
     * Same URL always produces the same short code.
     *
     * @param url The original URL
     * @return A deterministic short code
     */
    public String generateFromUrl(String url) {
        byte[] hash = sha256.digest(url.getBytes());
        long hashValue = bytesToLong(hash);
        return Base62Encoder.encode(Math.abs(hashValue)).substring(0, shortCodeLength);
    }

    /**
     * Generates a short code from a database ID using Base62 encoding.
     * This is the recommended approach for URL shortening.
     *
     * @param id The database ID
     * @return An encoded short code
     */
    public String generateFromId(long id) {
        return Base62Encoder.encode(id);
    }

    /**
     * Encodes a database ID to a specific-length short code.
     *
     * @param id The database ID
     * @param length The desired length
     * @return An encoded short code of specified length
     */
    public String encodeId(long id, int length) {
        String encoded = Base62Encoder.encode(id);
        if (encoded.length() > length) {
            return encoded.substring(encoded.length() - length);
        }
        return String.format("%" + length + "s", encoded).replace(' ', '0');
    }

    public boolean isValidUrl(String url) {
        return url != null && !url.isBlank() && VALID_URL_PATTERN.matcher(url).matches();
    }

    private long bytesToLong(byte[] bytes) {
        long value = 0;
        int length = Math.min(8, bytes.length);
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
    }
}