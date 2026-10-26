package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@ApplicationScoped
public class PasswordHasher {
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_SCHEME = "pbkdf2-sha256";
    private static final String HASH_VERSION = "v1";
    private static final int HASH_ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BYTES = KEY_LENGTH_BITS / Byte.SIZE;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derivedPassword = derive(password, salt);
        try {
            return String.join("$",
                    HASH_SCHEME,
                    HASH_VERSION,
                    Integer.toString(HASH_ITERATIONS),
                    Base64.getEncoder().withoutPadding().encodeToString(salt),
                    Base64.getEncoder().withoutPadding().encodeToString(derivedPassword));
        } finally {
            Arrays.fill(derivedPassword, (byte) 0);
        }
    }

    public boolean verify(String password, String passwordHash) {
        if (password == null || passwordHash == null) {
            throw new IllegalArgumentException("Password and password hash are required");
        }

        String[] components = passwordHash.split("\\$", -1);
        if (components.length != 5
                || !HASH_SCHEME.equals(components[0])
                || !HASH_VERSION.equals(components[1])) {
            throw new IllegalArgumentException("Unsupported password hash format");
        }

        int iterations;
        try {
            iterations = Integer.parseInt(components[2]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Invalid password hash iteration count", error);
        }
        if (iterations != HASH_ITERATIONS) {
            throw new IllegalArgumentException("Unsupported password hash iteration count");
        }

        byte[] salt = decodeComponent(components[3]);
        byte[] expectedHash = decodeComponent(components[4]);
        if (salt.length != SALT_LENGTH_BYTES || expectedHash.length != HASH_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid password hash component size");
        }

        byte[] actualHash = derive(password, salt);
        try {
            return MessageDigest.isEqual(expectedHash, actualHash);
        } finally {
            Arrays.fill(actualHash, (byte) 0);
        }
    }

    private byte[] decodeComponent(String component) {
        try {
            return Base64.getDecoder().decode(component);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid password hash encoding", error);
        }
    }

    private byte[] derive(String password, byte[] salt) {
        char[] passwordCharacters = password.toCharArray();
        PBEKeySpec keySpec = new PBEKeySpec(passwordCharacters, salt, HASH_ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(HASH_ALGORITHM).generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Unable to derive password hash", error);
        } finally {
            keySpec.clearPassword();
            Arrays.fill(passwordCharacters, '\0');
        }
    }
}
