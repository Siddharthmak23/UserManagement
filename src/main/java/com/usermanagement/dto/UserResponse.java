package com.usermanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.usermanagement.model.User;

import java.time.LocalDateTime;

/**
 * Safe user representation returned by the API. There is intentionally no
 * password or password-hash component.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
