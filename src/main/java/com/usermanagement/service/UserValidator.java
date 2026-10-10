package com.usermanagement.service;

import com.usermanagement.dto.UserRequest;
import com.usermanagement.model.Role;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Server-side field rules. Phone is required. Passwords are required when
 * creating a user and optional when updating one.
 */
@Component
public class UserValidator {

    static final String NAME_REQUIRED = "Name is required";
    static final String NAME_LENGTH = "Name must be between 2 and 100 characters";
    static final String NAME_CHARS = "Name can contain only letters, spaces, apostrophes, periods, and hyphens";
    static final String EMAIL_REQUIRED = "Email is required";
    static final String EMAIL_INVALID = "Enter a valid email address";
    static final String EMAIL_LENGTH = "Email must be 150 characters or fewer";
    static final String PHONE_REQUIRED = "Phone is required";
    static final String PHONE_INVALID = "Enter a valid phone number with 7 to 15 digits";
    static final String PASSWORD_REQUIRED = "Password is required";
    static final String PASSWORD_POLICY =
            "Password must be 8 to 72 characters and include uppercase, lowercase, and a number";
    static final String ROLE_REQUIRED = "Role is required";
    static final String ROLE_INVALID = "Role must be ADMIN or USER";

    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'\\-]{1,99}$");
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\- ]{7,20}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$");

    public Map<String, String> validateCreate(UserRequest request) {
        Map<String, String> errors = validateCommon(request);
        validatePassword(request == null ? null : request.getPassword(), true, errors);
        return errors;
    }

    public Map<String, String> validateUpdate(UserRequest request) {
        Map<String, String> errors = validateCommon(request);
        String password = request == null ? null : request.getPassword();
        if (password != null && !password.isEmpty()) {
            validatePassword(password, false, errors);
        }
        return errors;
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    public String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    public String normalizeRole(String role) {
        return Role.parse(role).map(Enum::name).orElse(null);
    }

    private Map<String, String> validateCommon(UserRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            errors.put("body", "Request body is required");
            return errors;
        }
        String name = normalizeName(request.getName());
        if (name == null || name.isEmpty()) {
            errors.put("name", NAME_REQUIRED);
        } else if (name.length() < 2 || name.length() > 100) {
            errors.put("name", NAME_LENGTH);
        } else if (!NAME.matcher(name).matches()) {
            errors.put("name", NAME_CHARS);
        }

        String email = normalizeEmail(request.getEmail());
        if (email == null || email.isEmpty()) {
            errors.put("email", EMAIL_REQUIRED);
        } else if (email.length() > 150) {
            errors.put("email", EMAIL_LENGTH);
        } else if (!EMAIL.matcher(email).matches()) {
            errors.put("email", EMAIL_INVALID);
        }

        String phone = normalizePhone(request.getPhone());
        if (phone == null || phone.isEmpty()) {
            errors.put("phone", PHONE_REQUIRED);
        } else if (!PHONE.matcher(phone).matches() || !digitCountOk(phone)) {
            errors.put("phone", PHONE_INVALID);
        }

        if (request.getRole() == null || request.getRole().isBlank()) {
            errors.put("role", ROLE_REQUIRED);
        } else if (normalizeRole(request.getRole()) == null) {
            errors.put("role", ROLE_INVALID);
        }
        return errors;
    }

    private void validatePassword(String password, boolean required, Map<String, String> errors) {
        if (password == null || password.isEmpty()) {
            if (required) {
                errors.put("password", PASSWORD_REQUIRED);
            }
            return;
        }
        if (!PASSWORD.matcher(password).matches()) {
            errors.put("password", PASSWORD_POLICY);
        }
    }

    private boolean digitCountOk(String phone) {
        long digits = phone.chars().filter(Character::isDigit).count();
        return digits >= 7 && digits <= 15;
    }
}
