package com.usermanagement.exception;

import java.util.Map;

/**
 * User-facing failure with an HTTP status. The message is safe to return to
 * the browser. Internal causes are logged separately and are not attached here.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final Map<String, String> errors;

    public ApiException(int status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(int status, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
