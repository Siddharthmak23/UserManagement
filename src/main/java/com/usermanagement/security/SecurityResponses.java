package com.usermanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class SecurityResponses {

    private SecurityResponses() {
    }

    public static boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        return path.equals("/api") || path.startsWith("/api/");
    }

    static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + escape(message) + "\"}");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
