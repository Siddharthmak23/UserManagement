package com.usermanagement.util;

import com.usermanagement.exception.ApiException;

import java.util.Map;

public final class QueryParameters {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;
    public static final int MAX_SEARCH_LENGTH = 100;

    private QueryParameters() {
    }

    public static int page(String raw) {
        return parse(raw, DEFAULT_PAGE, 1, Integer.MAX_VALUE, "page");
    }

    public static int size(String raw) {
        return parse(raw, DEFAULT_SIZE, 1, MAX_SIZE, "size");
    }

    public static String search(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            throw new ApiException(400, "Search must be 100 characters or fewer",
                    Map.of("search", "Search must be 100 characters or fewer"));
        }
        return trimmed;
    }

    private static int parse(String raw, int defaultValue, int min, int max, String name) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        final int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ApiException(400, name + " must be a whole number",
                    Map.of(name, name + " must be a whole number"));
        }
        if (value < min || value > max) {
            String message = name + " must be between " + min + " and " + (max == Integer.MAX_VALUE ? "a reasonable page" : max);
            if ("size".equals(name)) {
                message = "size must be between 1 and " + MAX_SIZE;
            }
            if ("page".equals(name)) {
                message = "page must be 1 or greater";
            }
            throw new ApiException(400, message, Map.of(name, message));
        }
        return value;
    }
}
