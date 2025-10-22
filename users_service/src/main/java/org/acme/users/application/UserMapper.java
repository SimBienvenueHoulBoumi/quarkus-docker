package org.acme.users.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.users.domain.model.User;
import org.acme.users.interfaces.rest.dto.UserResponse;

@ApplicationScoped
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail().getValue(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
