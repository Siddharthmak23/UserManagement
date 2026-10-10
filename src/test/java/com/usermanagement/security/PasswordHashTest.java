package com.usermanagement.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void matchingPasswordVerifiesAndADifferentPasswordDoesNot() {
        String hash = encoder.encode("Admin@12345");

        assertNotEquals("Admin@12345", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("Admin@12345", hash));
        assertFalse(encoder.matches("Admin@12346", hash));
        assertFalse(encoder.matches("' OR '1'='1", hash));
    }
}
