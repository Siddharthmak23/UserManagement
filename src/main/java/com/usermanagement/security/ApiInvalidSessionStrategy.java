package com.usermanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.session.InvalidSessionStrategy;

import java.io.IOException;

public class ApiInvalidSessionStrategy implements InvalidSessionStrategy {

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SecurityResponses.isApiRequest(request)) {
            SecurityResponses.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/login?expired");
    }
}
