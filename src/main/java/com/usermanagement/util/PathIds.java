package com.usermanagement.util;

import com.usermanagement.exception.ApiException;

public final class PathIds {

    private PathIds() {
    }

    public static long parseUserId(String raw) {
        if (raw == null || !raw.matches("\\d{1,18}")) {
            throw new ApiException(400, "User id must be a positive number");
        }
        long value = Long.parseLong(raw);
        if (value < 1) {
            throw new ApiException(400, "User id must be a positive number");
        }
        return value;
    }
}
