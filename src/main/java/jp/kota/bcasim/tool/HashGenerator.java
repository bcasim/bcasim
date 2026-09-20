package jp.kota.bcasim.tool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashGenerator {
    private HashGenerator() {}
    public static String generateHash(String data) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[bytes.length * 2];
            String alphabet = "0123456789abcdef";
            for (int i = 0; i < bytes.length; i++) {
                hex[i * 2] = alphabet.charAt((bytes[i] & 255) >>> 4);
                hex[i * 2 + 1] = alphabet.charAt(bytes[i] & 15);
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
