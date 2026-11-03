package br.com.documentshield.security;

import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class AdminPasswordHashTool {
    private AdminPasswordHashTool() {}

    public static void main(String[] args) {
        String supplied = System.getenv("DOCUMENT_SHIELD_ADMIN_PASSWORD");
        char[] password = supplied == null ? null : supplied.toCharArray();
        try {
            if (password == null || password.length < 12) {
                System.err.println(
                        "Defina DOCUMENT_SHIELD_ADMIN_PASSWORD com pelo menos 12 caracteres apenas durante este comando.");
                System.exit(2);
            }
            System.out.println(new BCryptPasswordEncoder(12).encode(new String(password)));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
