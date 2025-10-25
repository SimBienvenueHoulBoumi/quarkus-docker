package org.acme.users.presentation.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.users.application.UserService;
import org.acme.users.application.exception.UserApplicationException;
import org.acme.users.application.dto.response.AuthResponse;
import org.acme.users.application.dto.request.LoginRequest;
import org.acme.users.application.dto.request.RegisterRequest;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    UserService userService;

    @POST
    @Path("/register")
    public AuthResponse register(@Valid RegisterRequest request) {
        return execute(() -> userService.register(request));
    }

    @POST
    @Path("/register/admin")
    public AuthResponse registerAdmin(@Valid RegisterRequest request) {
        return execute(() -> userService.registerAdmin(request));
    }

    @POST
    @Path("/login")
    public AuthResponse login(@Valid LoginRequest request) {
        return execute(() -> userService.login(request));
    }

    private AuthResponse execute(java.util.function.Supplier<AuthResponse> action) {
        try {
            return action.get();
        } catch (UserApplicationException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.status(ex.getStatusCode()).build());
        }
    }
}
