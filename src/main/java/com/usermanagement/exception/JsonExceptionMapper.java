package com.usermanagement.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.usermanagement.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

@Component
@Provider
public class JsonExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Request body is invalid", null))
                .build();
    }
}
