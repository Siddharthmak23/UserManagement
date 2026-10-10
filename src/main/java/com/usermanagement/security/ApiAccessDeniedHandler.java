package com.usermanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

/**
 * Returns HTTP 403 JSON for API denials and CSRF failures. Page requests go to
 * a forbidden view so a signed-in non-administrator is not bounced back to login.
 */
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (SecurityResponses.isApiRequest(request)) {
            String message = accessDeniedException instanceof CsrfException
                    ? "Invalid or missing CSRF token"
                    : "You do not have permission to perform this action";
            SecurityResponses.writeJson(response, HttpServletResponse.SC_FORBIDDEN, message);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/forbidden");
    }
}
