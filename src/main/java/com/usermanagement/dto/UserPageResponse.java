package com.usermanagement.dto;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> users,
        int page,
        int size,
        long total,
        int totalPages
) {
}
