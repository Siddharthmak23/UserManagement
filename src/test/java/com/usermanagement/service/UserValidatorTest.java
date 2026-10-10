package com.usermanagement.service;

import com.usermanagement.dto.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidatorTest {

    private UserValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UserValidator();
    }

    @Test
    void acceptsACompleteUser() {
        assertTrue(validator.validateCreate(validRequest()).isEmpty());
    }

    @Test
    void requiresFieldsAndRejectsMalformedValues() {
        UserRequest request = new UserRequest();
        request.setName(" ");
        request.setEmail("not-an-email");
        request.setPhone("12");
        request.setPassword("short");
        request.setRole("GUEST");

        Map<String, String> errors = validator.validateCreate(request);

        assertEquals(UserValidator.NAME_REQUIRED, errors.get("name"));
        assertEquals(UserValidator.EMAIL_INVALID, errors.get("email"));
        assertEquals(UserValidator.PHONE_INVALID, errors.get("phone"));
        assertEquals(UserValidator.PASSWORD_POLICY, errors.get("password"));
        assertEquals(UserValidator.ROLE_INVALID, errors.get("role"));
    }

    @Test
    void updateAllowsAnEmptyPasswordAndRejectsAWeakOne() {
        UserRequest blankPassword = validRequest();
        blankPassword.setPassword("");
        assertFalse(validator.validateUpdate(blankPassword).containsKey("password"));

        UserRequest weakPassword = validRequest();
        weakPassword.setPassword("alllowercase1");
        assertEquals(UserValidator.PASSWORD_POLICY, validator.validateUpdate(weakPassword).get("password"));
    }

    @Test
    void normalizesEmailAndName() {
        assertEquals("ada@example.com", validator.normalizeEmail("  Ada@Example.com "));
        assertEquals("Ada Lovelace", validator.normalizeName("  Ada   Lovelace "));
        assertEquals("ADMIN", validator.normalizeRole(" admin "));
    }

    @Test
    void rejectsNamesThatCouldCarryMarkup() {
        UserRequest request = validRequest();
        request.setName("<script>alert(1)</script>");
        assertEquals(UserValidator.NAME_CHARS, validator.validateCreate(request).get("name"));
    }

    private static UserRequest validRequest() {
        UserRequest request = new UserRequest();
        request.setName("Ada Lovelace");
        request.setEmail("ada@example.com");
        request.setPhone("(555) 010-0199");
        request.setPassword("Secret123");
        request.setRole("USER");
        return request;
    }
}
