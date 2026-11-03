package br.com.documentshield.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyTokenGenerator {
    private static final int SECRET_BYTES = 32;
    private static final int PUBLIC_ID_BYTES = 16;
    private final SecureRandom random;

    public ApiKeyTokenGenerator() {
        this(new SecureRandom());
    }

    ApiKeyTokenGenerator(SecureRandom random) {
        this.random = random;
    }

    public GeneratedApiKey generate() {
        byte[] publicId = new byte[PUBLIC_ID_BYTES];
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(publicId);
        random.nextBytes(secret);
        String keyId = HexFormat.of().formatHex(publicId);
        String encodedSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        return new GeneratedApiKey(keyId, "dsk_" + keyId + "." + encodedSecret);
    }

    public record GeneratedApiKey(String keyId, String token) {}
}
