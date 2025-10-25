package org.acme.gateway.presentation.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.acme.gateway.infrastructure.client.UsersServiceClient;
import org.acme.gateway.application.dto.response.AuthResponse;
import org.acme.gateway.application.dto.request.LoginRequest;
import org.acme.gateway.application.dto.request.RegisterRequest;
import org.acme.gateway.application.dto.request.UpdateUserRequest;
import org.acme.gateway.application.dto.response.UserResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.util.List;
import java.util.function.Supplier;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsersGatewayResource {

    @Inject
    @RestClient
    UsersServiceClient usersServiceClient;

    // Auth endpoints
    @POST
    @Path("/auth/register")
    public AuthResponse register(@Valid RegisterRequest request) {
        return invoke(() -> usersServiceClient.register(request));
    }

    @POST
    @Path("/auth/login")
    public AuthResponse login(@Valid LoginRequest request) {
        return invoke(() -> usersServiceClient.login(request));
    }

    // User endpoints
    @GET
    @Path("/users/me")
    public UserResponse getCurrentUser(@Context HttpHeaders headers) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return invoke(() -> usersServiceClient.getCurrentUser(authorization));
    }

    @PUT
    @Path("/users/me")
    public UserResponse updateCurrentUser(@Context HttpHeaders headers, @Valid UpdateUserRequest request) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return invoke(() -> usersServiceClient.updateCurrentUser(authorization, request));
    }

    @GET
    @Path("/users")
    public List<UserResponse> getAllUsers(@Context HttpHeaders headers) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return invoke(() -> usersServiceClient.getAllUsers(authorization));
    }

    @GET
    @Path("/users/{id}")
    public UserResponse getUserById(@Context HttpHeaders headers, @PathParam("id") Long id) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return invoke(() -> usersServiceClient.getUserById(authorization, id));
    }

    private <T> T invoke(Supplier<T> action) {
        try {
            return action.get();
        } catch (ClientWebApplicationException ex) {
            throw translate(ex);
        }
    }

    private WebApplicationException translate(ClientWebApplicationException ex) {
        var response = ex.getResponse();
        if (response != null) {
            return new WebApplicationException(response);
        }
        return new WebApplicationException(ex);
    }
}
