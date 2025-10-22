package org.acme.users.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.users.application.UserService;
import org.acme.users.application.exception.UserApplicationException;
import org.acme.users.interfaces.rest.dto.UserResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    JsonWebToken principal;

    @Inject
    UserService userService;

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    public UserResponse currentUser() {
        Object claim = principal.getClaim("userId");
        if (claim == null) {
            throw new WebApplicationException("Missing userId claim", Response.Status.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(claim.toString());
        return execute(() -> userService.findUserProfile(userId));
    }

    private <T> T execute(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (UserApplicationException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.status(ex.getStatusCode()).build());
        }
    }
}
