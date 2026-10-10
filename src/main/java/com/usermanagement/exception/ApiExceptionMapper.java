package com.usermanagement.exception;

import com.usermanagement.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(ApiException exception) {
        if (exception.getStatus() >= 500) {
            log.error("API failure: {}", exception.getMessage(), exception);
        }
        Map<String, String> errors = exception.getErrors().isEmpty() ? null : exception.getErrors();
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(exception.getMessage(), errors))
                .build();
    }
}
