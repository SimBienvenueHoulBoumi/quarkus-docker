package org.acme.gateway.application.usecase.users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.acme.gateway.application.dto.request.LoginRequest;
import org.acme.gateway.application.dto.request.RegisterRequest;
import org.acme.gateway.application.dto.request.UpdateUserRequest;
import org.acme.gateway.application.dto.response.AuthResponse;
import org.acme.gateway.application.dto.response.UserResponse;
import org.acme.gateway.infrastructure.client.UsersServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.util.List;
import java.util.function.Supplier;

@ApplicationScoped
public class UsersGatewayService {

    private final UsersServiceClient usersServiceClient;

    public UsersGatewayService(@RestClient UsersServiceClient usersServiceClient) {
        this.usersServiceClient = usersServiceClient;
    }

    public AuthResponse register(RegisterRequest request) {
        return invoke(() -> usersServiceClient.register(request));
    }

    public AuthResponse login(LoginRequest request) {
        return invoke(() -> usersServiceClient.login(request));
    }

    public UserResponse getCurrentUser(String authorization) {
        return invoke(() -> usersServiceClient.getCurrentUser(authorization));
    }

    public UserResponse updateCurrentUser(String authorization, UpdateUserRequest request) {
        return invoke(() -> usersServiceClient.updateCurrentUser(authorization, request));
    }

    public List<UserResponse> getAllUsers(String authorization) {
        return invoke(() -> usersServiceClient.getAllUsers(authorization));
    }

    public UserResponse getUserById(String authorization, Long id) {
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
