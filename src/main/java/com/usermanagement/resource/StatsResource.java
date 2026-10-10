package com.usermanagement.resource;

import com.usermanagement.dto.StatsResponse;
import com.usermanagement.exception.ApiException;
import com.usermanagement.security.SecurityUtils;
import com.usermanagement.service.UserService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

@Component
@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {

    private final UserService userService;

    public StatsResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    public StatsResponse stats() {
        if (!"ADMIN".equals(SecurityUtils.currentUser().getRole())) {
            throw new ApiException(403, "You do not have permission to perform this action");
        }
        return userService.stats();
    }
}
