package com.usermanagement.security;

import com.usermanagement.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserPrincipalTest {

    @Test
    void sessionPrincipalDropsThePasswordHashAfterAuthentication() {
        User user = new User(3L, "Ada Lovelace", "ada@example.com", "5550100199",
                "$2a$10$hash", "ADMIN", null, null);
        AppUserPrincipal principal = new AppUserPrincipal(user);

        assertEquals("ada@example.com", principal.getUsername());
        assertEquals("ROLE_ADMIN", principal.getAuthorities().iterator().next().getAuthority());
        assertEquals("$2a$10$hash", principal.getPassword());

        principal.eraseCredentials();

        assertNull(principal.getPassword());
        assertTrue(principal.toString().contains("ada@example.com"));
        assertFalseContainsHash(principal.toString());
    }

    private static void assertFalseContainsHash(String value) {
        org.junit.jupiter.api.Assertions.assertFalse(value.contains("$2a$"));
    }
}
