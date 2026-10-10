package com.usermanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

/**
 * Sends anonymous page visitors to the login page and returns HTTP 401 for the API.
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final LoginUrlAuthenticationEntryPoint loginEntryPoint =
            new LoginUrlAuthenticationEntryPoint("/login");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, jakarta.servlet.ServletException {
        if (SecurityResponses.isApiRequest(request)) {
            SecurityResponses.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }
        loginEntryPoint.commence(request, response, authException);
    }
}
