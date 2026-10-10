package com.usermanagement.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.usermanagement.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = LoggerFactory.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        ApiException apiException = findCause(exception, ApiException.class);
        if (apiException != null) {
            return new ApiExceptionMapper().toResponse(apiException);
        }
        if (findCause(exception, JsonProcessingException.class) != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Request body is invalid", null))
                    .build();
        }
        log.error("Unhandled request failure", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("An unexpected error occurred", null))
                .build();
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
