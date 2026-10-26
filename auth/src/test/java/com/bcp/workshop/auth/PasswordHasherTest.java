package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void generatesDifferentSaltForTheSamePassword() {
        String firstHash = passwordHasher.hash("Workshop123!");
        String secondHash = passwordHasher.hash("Workshop123!");

        assertNotEquals(firstHash, secondHash);
        assertNotEquals(firstHash.split("\\$")[3], secondHash.split("\\$")[3]);
    }

    @Test
    void verifiesOnlyTheMatchingPassword() {
        String passwordHash = passwordHasher.hash("Workshop123!");

        assertTrue(passwordHasher.verify("Workshop123!", passwordHash));
        assertFalse(passwordHasher.verify("incorrect", passwordHash));
    }

    @Test
    void rejectsUnsupportedHashFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.verify("Workshop123!", "unsupported"));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.verify(null, "hash"));
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.verify("password", null));
    }

    @Test
    void rejectsInvalidIterationCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.verify("password", "pbkdf2-sha256$v1$invalid$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.verify("password", "pbkdf2-sha256$v1$1$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
    }

    @Test
    void rejectsInvalidHashComponents() {
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.verify("password", "pbkdf2-sha256$v1$600000$%%%$%%%"));
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.verify("password", "pbkdf2-sha256$v1$600000$YQ$YQ"));
    }
}
