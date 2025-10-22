package org.acme.users.application;

import org.acme.users.interfaces.rest.dto.AuthResponse;
import org.acme.users.interfaces.rest.dto.LoginRequest;
import org.acme.users.interfaces.rest.dto.RegisterRequest;
import org.acme.users.interfaces.rest.dto.UserResponse;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse findUserProfile(Long userId);
}
