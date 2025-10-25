package org.acme.gateway.infrastructure.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.gateway.application.dto.response.AuthResponse;
import org.acme.gateway.application.dto.request.LoginRequest;
import org.acme.gateway.application.dto.request.RegisterRequest;
import org.acme.gateway.application.dto.request.UpdateUserRequest;
import org.acme.gateway.application.dto.response.UserResponse;

import java.util.List;

@Path("/api")
public interface UsersServiceClient {

    // Auth endpoints
    @POST
    @Path("/auth/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    AuthResponse register(RegisterRequest request);

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    AuthResponse login(LoginRequest request);

    // User endpoints
    @GET
    @Path("/users/me")
    @Produces(MediaType.APPLICATION_JSON)
    UserResponse getCurrentUser(@HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/users/me")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    UserResponse updateCurrentUser(@HeaderParam("Authorization") String authorization, UpdateUserRequest request);

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserResponse> getAllUsers(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    UserResponse getUserById(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id);
}
