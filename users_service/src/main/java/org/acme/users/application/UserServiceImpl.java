package org.acme.users.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.users.application.exception.UserApplicationException;
import org.acme.users.domain.model.User;
import org.acme.users.domain.model.UserRole;
import org.acme.users.domain.repository.UserRepository;
import org.acme.users.domain.value.Email;
import org.acme.users.infrastructure.security.JwtService;
import org.acme.users.infrastructure.security.JwtService.TokenWithExpiry;
import org.acme.users.application.dto.response.AuthResponse;
import org.acme.users.application.dto.request.LoginRequest;
import org.acme.users.application.dto.request.RegisterRequest;
import org.acme.users.application.dto.request.UpdateUserRequest;
import org.acme.users.application.dto.response.UserResponse;

import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Inject
    public UserServiceImpl(UserRepository userRepository,
                           JwtService jwtService,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    @Override
    public AuthResponse register(RegisterRequest request) {
        return registerWithRole(request, UserRole.USER);
    }

    @Transactional
    @Override
    public AuthResponse registerAdmin(RegisterRequest request) {
        return registerWithRole(request, UserRole.ADMIN);
    }

    private AuthResponse registerWithRole(RegisterRequest request, UserRole role) {
        userRepository.findByUsername(request.username()).ifPresent(existing -> {
            throw new UserApplicationException("username '%s' is already taken".formatted(request.username()), 409);
        });

        Email email = Email.of(request.email());

        userRepository.findByEmail(email.getValue()).ifPresent(existing -> {
            throw new UserApplicationException("email '%s' is already registered".formatted(request.email()), 409);
        });

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(hashPassword(request.password()));
        user.setRole(role);

        userRepository.persist(user);

        TokenWithExpiry token = jwtService.issueToken(user);
        return AuthResponse.bearer(token.token(), token.duration().toSeconds());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.username())
                .orElseThrow(() -> new UserApplicationException("invalid credentials", 401));

        if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            throw new UserApplicationException("invalid credentials", 401);
        }

        TokenWithExpiry token = jwtService.issueToken(user);
        return AuthResponse.bearer(token.token(), token.duration().toSeconds());
    }

    @Override
    public UserResponse findUserProfile(Long userId) {
        return userMapper.toResponse(getUserOrThrow(userId));
    }

    @Transactional
    @Override
    public UserResponse updateUserProfile(Long userId, UpdateUserRequest request) {
        User user = getUserOrThrow(userId);

        if (hasText(request.username()) && !request.username().equals(user.getUsername())) {
            userRepository.findByUsername(request.username()).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new UserApplicationException(
                            "username '%s' is already taken".formatted(request.username()), 409);
                }
            });
            user.setUsername(request.username());
        }

        if (hasText(request.email())) {
            Email email = Email.of(request.email());
            userRepository.findByEmail(email.getValue()).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new UserApplicationException(
                            "email '%s' is already registered".formatted(request.email()), 409);
                }
            });
            user.setEmail(email);
        }

        if (hasText(request.password())) {
            user.setPasswordHash(hashPassword(request.password()));
        }

        userRepository.persist(user);
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> findAllUsers() {
        return userRepository.listAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse findUserById(Long userId) {
        return userMapper.toResponse(getUserOrThrow(userId));
    }

    private String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findOptionalById(userId)
                .orElseThrow(() -> new UserApplicationException("User not found", 404));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
