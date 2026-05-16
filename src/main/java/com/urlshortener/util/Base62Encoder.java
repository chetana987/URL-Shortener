package com.urlshortener.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    static final char[] DIGITS = BASE62_CHARS.toCharArray();
    private static final MessageDigest MD5;

    static {
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Encodes a long ID into a Base62 string.
     * Example: 1254376890 -> "8S2kL"
     *
     * @param id The numeric ID to encode
     * @return Base62 encoded string
     */
    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative");
        }

        if (id == 0) {
            return String.valueOf(DIGITS[0]);
        }

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(DIGITS[(int) (id % BASE)]);
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to its numeric ID.
     * Example: "8S2kL" -> 1254376890
     *
     * @param encoded The Base62 encoded string
     * @return The decoded numeric ID
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < encoded.length(); i++) {
            int digit = BASE62_CHARS.indexOf(encoded.charAt(i));
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character in encoded string: " + encoded.charAt(i));
            }
            result = result * BASE + digit;
        }
        return result;
    }

    /**
     * Generates a short code from any string using MD5 hash.
     * Takes first N characters of the Base62 encoded MD5 hash.
     * Useful for generating consistent short codes from custom aliases.
     *
     * @param input The input string (e.g., custom alias)
     * @param length The desired short code length
     * @return A short code of specified length
     */
    public static String encodeString(String input, int length) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        byte[] hash = MD5.digest(input.getBytes());
        long value = bytesToLong(hash);
        return encode(value).substring(0, Math.min(length, encode(value).length()));
    }

    /**
     * Generates a unique short code with timestamp component.
     * Format: {timestamp-base62}-{random-base62}
     *
     * @param length Length of random component
     * @return A time-based unique short code
     */
    public static String generateTimeBasedCode(int length) {
        long timestamp = System.currentTimeMillis();
        long random = (long) (Math.random() * Math.pow(BASE, length));
        return encode(timestamp) + encode(random).substring(0, length);
    }

    /**
     * Validates if a string is a valid Base62 encoded string.
     *
     * @param encoded The string to validate
     * @return true if valid Base62, false otherwise
     */
    public static boolean isValidBase62(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return false;
        }
        for (char c : encoded.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts byte array to long using first 8 bytes.
     */
    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        int length = Math.min(8, bytes.length);
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return Math.abs(value);
    }

    public static void main(String[] args) {
        System.out.println("=== Base62 Encoder Demo ===\n");

        long[] testIds = {0, 1, 62, 100, 1000, 999999999, Long.MAX_VALUE};
        System.out.println("Encoding IDs:");
        for (long id : testIds) {
            try {
                String encoded = encode(id);
                long decoded = decode(encoded);
                System.out.printf("ID: %-20d -> Encoded: %-15s -> Decoded: %d%n", id, encoded, decoded);
            } catch (Exception e) {
                System.out.printf("ID: %d -> Error: %s%n", id, e.getMessage());
            }
        }

        System.out.println("\nValidating Base62 strings:");
        String[] testStrings = {"abc123", "XYZ789", "invalid-char!", "a]b"};
        for (String s : testStrings) {
            System.out.printf("'%s' is valid: %s%n", s, isValidBase62(s));
        }

        System.out.println("\nString encoding:");
        String input = "my-custom-alias";
        String shortCode = encodeString(input, 7);
        System.out.printf("Input: '%s' -> Short code: '%s'%n", input, shortCode);
    }
}