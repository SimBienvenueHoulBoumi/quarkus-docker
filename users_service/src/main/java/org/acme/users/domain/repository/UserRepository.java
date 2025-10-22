package org.acme.users.domain.repository;

import org.acme.users.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    void persist(User user);

    Optional<User> findOptionalById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
}
