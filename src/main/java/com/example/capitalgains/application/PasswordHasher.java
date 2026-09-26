package com.example.capitalgains.application;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2-HMAC-SHA256 password hashing straight from the JDK, so no security
 * library is pulled in. Stored format: {@code pbkdf2$<iterations>$<salt>$<hash>}.
 * The iteration count travels with the hash, so it can be raised later without
 * breaking existing accounts.
 */
@Component
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000; // OWASP 2023 recommendation for this algorithm
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(key);
    }

    public boolean matches(String password, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
            return false;
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] expected = b64.decode(parts[3]);
        byte[] actual = derive(password, b64.decode(parts[2]), Integer.parseInt(parts[1]));
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available in this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }
}
