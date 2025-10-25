package org.acme.users.presentation.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.users.application.UserService;
import org.acme.users.application.exception.UserApplicationException;
import org.acme.users.application.dto.request.UpdateUserRequest;
import org.acme.users.application.dto.response.UserResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.function.Supplier;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    JsonWebToken principal;

    @Inject
    UserService userService;

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    public UserResponse currentUser() {
        Long userId = getUserIdFromToken();
        return execute(() -> userService.findUserProfile(userId));
    }

    @PUT
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    public UserResponse updateCurrentUser(@Valid UpdateUserRequest request) {
        Long userId = getUserIdFromToken();
        return execute(() -> userService.updateUserProfile(userId, request));
    }

    @GET
    @RolesAllowed("ADMIN")
    public List<UserResponse> listUsers() {
        return execute(userService::findAllUsers);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public UserResponse getUserById(@PathParam("id") Long id) {
        return execute(() -> userService.findUserById(id));
    }

    private Long getUserIdFromToken() {
        Object claim = principal.getClaim("userId");
        if (claim == null) {
            throw new WebApplicationException("Missing userId claim", Response.Status.UNAUTHORIZED);
        }
        return Long.valueOf(claim.toString());
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (UserApplicationException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.status(ex.getStatusCode()).build());
        }
    }
}
