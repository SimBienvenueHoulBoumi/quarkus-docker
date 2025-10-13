package org.acme.users.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.users.service.UserService;
import org.acme.users.web.dto.AuthResponse;
import org.acme.users.web.dto.LoginRequest;
import org.acme.users.web.dto.RegisterRequest;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    UserService userService;

    @POST
    @Path("/register")
    public AuthResponse register(@Valid RegisterRequest request) {
        return userService.register(request);
    }

    @POST
    @Path("/login")
    public AuthResponse login(@Valid LoginRequest request) {
        return userService.login(request);
    }
}
