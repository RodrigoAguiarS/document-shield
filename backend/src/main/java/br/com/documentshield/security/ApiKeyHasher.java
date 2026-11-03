package br.com.documentshield.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyHasher {
    public String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public boolean matches(String token, String expectedHash) {
        if (expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(token).getBytes(StandardCharsets.US_ASCII), expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
