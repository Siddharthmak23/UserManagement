package com.usermanagement.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.usermanagement.exception.ApiExceptionMapper;
import com.usermanagement.exception.GenericExceptionMapper;
import com.usermanagement.model.User;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafeResponseTest {

    @Test
    void userJsonOmitsThePasswordHash() throws Exception {
        User user = new User(1L, "Ada", "ada@example.com", "5550100199",
                "$2a$10$secret-hash", "USER",
                LocalDateTime.of(2026, 10, 9, 12, 0),
                LocalDateTime.of(2026, 10, 9, 12, 30));
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writeValueAsString(UserResponse.from(user));

        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret-hash"));
        assertFalse(json.contains("$2a$"));
    }

    @Test
    void unexpectedErrorsDoNotLeakInternalDetails() {
        Response response = new GenericExceptionMapper()
                .toResponse(new IllegalStateException("SELECT password_hash FROM users WHERE email = 'root'"));
        ErrorResponse body = (ErrorResponse) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("An unexpected error occurred", body.message());
        assertNull(body.errors());
        assertFalse(body.message().contains("password_hash"));
    }

    @Test
    void apiErrorsKeepTheStatusAndFieldMessages() {
        Response response = new ApiExceptionMapper().toResponse(
                new com.usermanagement.exception.ApiException(409, "A user with this email already exists",
                        java.util.Map.of("email", "A user with this email already exists")));
        ErrorResponse body = (ErrorResponse) response.getEntity();

        assertEquals(409, response.getStatus());
        assertEquals("A user with this email already exists", body.errors().get("email"));
    }
}
