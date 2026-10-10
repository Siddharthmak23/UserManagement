package com.usermanagement.exception;

import com.usermanagement.dto.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

@Component
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse() == null ? 500 : exception.getResponse().getStatus();
        String message = switch (status) {
            case 400 -> "The request is invalid";
            case 404 -> "Resource not found";
            case 405 -> "Method not allowed";
            case 415 -> "Content type must be application/json";
            default -> "Request failed";
        };
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(message, null))
                .build();
    }
}
