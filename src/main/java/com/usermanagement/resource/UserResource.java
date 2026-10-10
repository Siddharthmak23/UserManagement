package com.usermanagement.resource;

import com.usermanagement.dto.UserPageResponse;
import com.usermanagement.dto.UserRequest;
import com.usermanagement.dto.UserResponse;
import com.usermanagement.exception.ApiException;
import com.usermanagement.security.AppUserPrincipal;
import com.usermanagement.security.SecurityUtils;
import com.usermanagement.service.UserService;
import com.usermanagement.util.PathIds;
import com.usermanagement.util.QueryParameters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

@Component
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    public UserPageResponse list(@QueryParam("page") @DefaultValue("1") String page,
                                 @QueryParam("size") @DefaultValue("10") String size,
                                 @QueryParam("search") String search) {
        requireAdmin();
        return userService.list(
                QueryParameters.page(page),
                QueryParameters.size(size),
                QueryParameters.search(search));
    }

    @GET
    @Path("/{id}")
    public UserResponse get(@PathParam("id") String id) {
        requireAdmin();
        return userService.get(PathIds.parseUserId(id));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(UserRequest request) {
        requireAdmin();
        return Response.status(Response.Status.CREATED)
                .type(MediaType.APPLICATION_JSON)
                .entity(userService.create(request))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public UserResponse update(@PathParam("id") String id, UserRequest request) {
        AppUserPrincipal actor = requireAdmin();
        long userId = PathIds.parseUserId(id);
        UserResponse updated = userService.update(userId, request, actor.getId());
        if (actor.getId().equals(userId)) {
            actor.setName(updated.name());
            actor.setEmail(updated.email());
        }
        return updated;
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        AppUserPrincipal actor = requireAdmin();
        userService.delete(PathIds.parseUserId(id), actor.getId());
        return Response.noContent().build();
    }

    private AppUserPrincipal requireAdmin() {
        AppUserPrincipal principal = SecurityUtils.currentUser();
        if (!"ADMIN".equals(principal.getRole())) {
            throw new ApiException(403, "You do not have permission to perform this action");
        }
        return principal;
    }
}
