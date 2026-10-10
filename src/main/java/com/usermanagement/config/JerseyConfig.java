package com.usermanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.exception.ApiExceptionMapper;
import com.usermanagement.exception.GenericExceptionMapper;
import com.usermanagement.exception.JsonExceptionMapper;
import com.usermanagement.exception.ObjectMapperContextResolver;
import com.usermanagement.exception.WebApplicationExceptionMapper;
import com.usermanagement.resource.StatsResource;
import com.usermanagement.resource.UserResource;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.stereotype.Component;

/**
 * Jersey resources under {@code /api}. The path is also set with
 * {@code spring.jersey.application-path} so Spring MVC keeps the JSP routes.
 */
@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(ObjectMapper objectMapper,
                        UserResource userResource,
                        StatsResource statsResource,
                        ApiExceptionMapper apiExceptionMapper,
                        JsonExceptionMapper jsonExceptionMapper,
                        WebApplicationExceptionMapper webApplicationExceptionMapper,
                        GenericExceptionMapper genericExceptionMapper) {
        register(new ObjectMapperContextResolver(objectMapper));
        register(JacksonFeature.class);
        register(userResource);
        register(statsResource);
        register(apiExceptionMapper);
        register(jsonExceptionMapper);
        register(webApplicationExceptionMapper);
        register(genericExceptionMapper);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, false);
    }
}
