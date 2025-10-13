package org.acme.users.web;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.users.service.UserService;
import org.acme.users.web.dto.UserResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.ws.rs.WebApplicationException;

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
        var entity = userService.findByIdOrThrow(userId);
        return userService.toResponse(entity);
    }
}
