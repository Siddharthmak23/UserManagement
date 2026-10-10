package com.usermanagement.model;

import java.util.Locale;
import java.util.Optional;

public enum Role {
    ADMIN,
    USER;

    public static Optional<Role> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
