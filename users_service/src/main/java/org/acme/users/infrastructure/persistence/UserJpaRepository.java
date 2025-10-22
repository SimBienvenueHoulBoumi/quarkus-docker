package org.acme.users.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.acme.users.domain.model.User;
import org.acme.users.domain.repository.UserRepository;

import java.util.Optional;

@ApplicationScoped
public class UserJpaRepository implements UserRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public void persist(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
        } else {
            entityManager.merge(user);
        }
    }

    @Override
    public Optional<User> findOptionalById(Long id) {
        return Optional.ofNullable(entityManager.find(User.class, id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery("select u from User u where u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return entityManager.createQuery("select u from User u where u.email.value = :email", User.class)
                .setParameter("email", email.toLowerCase())
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        String lowered = usernameOrEmail.toLowerCase();
        return entityManager.createQuery(
                        "select u from User u where lower(u.email.value) = :email or u.username = :username",
                        User.class)
                .setParameter("email", lowered)
                .setParameter("username", usernameOrEmail)
                .getResultStream()
                .findFirst();
    }
}
