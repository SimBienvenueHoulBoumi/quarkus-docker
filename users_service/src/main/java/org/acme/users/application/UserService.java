package org.acme.users.application;

import org.acme.users.application.dto.response.AuthResponse;
import org.acme.users.application.dto.request.LoginRequest;
import org.acme.users.application.dto.request.RegisterRequest;
import org.acme.users.application.dto.request.UpdateUserRequest;
import org.acme.users.application.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse registerAdmin(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse findUserProfile(Long userId);

    UserResponse updateUserProfile(Long userId, UpdateUserRequest request);

    List<UserResponse> findAllUsers();

    UserResponse findUserById(Long userId);
}
