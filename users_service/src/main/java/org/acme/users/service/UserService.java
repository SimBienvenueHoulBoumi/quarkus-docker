package org.acme.users.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.acme.users.domain.UserEntity;
import org.acme.users.domain.UserRole;
import org.acme.users.repository.UserRepository;
import org.acme.users.web.dto.AuthResponse;
import org.acme.users.web.dto.LoginRequest;
import org.acme.users.web.dto.RegisterRequest;
import org.acme.users.web.dto.UserResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.text.MessageFormat;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(existing -> {
            throw new WebApplicationException(
                    MessageFormat.format("username ''{0}'' is already taken", request.username()),
                    Response.Status.CONFLICT);
        });

        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new WebApplicationException(
                    MessageFormat.format("email ''{0}'' is already registered", request.email()),
                    Response.Status.CONFLICT);
        });

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(hashPassword(request.password()));
        user.setRole(UserRole.USER);

        userRepository.persist(user);
        if (!userRepository.isPersistent(user)) {
            throw new WebApplicationException("Unable to persist user", Response.Status.INTERNAL_SERVER_ERROR);
        }

        var token = jwtService.issueToken(user);
        return AuthResponse.bearer(token.token(), token.duration().toSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByUsernameOrEmail(request.identifier())
                .orElseThrow(() -> new WebApplicationException("invalid credentials", Response.Status.UNAUTHORIZED));

        if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            throw new WebApplicationException("invalid credentials", Response.Status.UNAUTHORIZED);
        }

        var token = jwtService.issueToken(user);
        return AuthResponse.bearer(token.token(), token.duration().toSeconds());
    }

    public UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    public UserEntity findByIdOrThrow(Long id) {
        return userRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    private String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }
}
