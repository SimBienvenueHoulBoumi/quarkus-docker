package org.acme.notifications.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.acme.notifications.domain.model.Notification;
import org.acme.notifications.domain.repository.NotificationRepository;

@ApplicationScoped
public class NotificationJpaRepository implements NotificationRepository {

    @PersistenceContext
    EntityManager entityManager;

    public void persist(Notification notification) {
        if (notification.getId() == null) {
            entityManager.persist(notification);
        } else {
            entityManager.merge(notification);
        }
    }

    public Optional<Notification> findOptionalById(Long id) {
        return Optional.ofNullable(entityManager.find(Notification.class, id));
    }

    public List<Notification> findByUserId(Long userId) {
        return entityManager.createQuery(
                        "select n from Notification n where n.userId = :userId order by n.createdAt desc",
                        Notification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Notification> findUnreadByUserId(Long userId) {
        return entityManager.createQuery(
                        "select n from Notification n where n.userId = :userId and n.isRead = false order by n.createdAt desc",
                        Notification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public long countUnreadByUserId(Long userId) {
        return entityManager.createQuery(
                        "select count(n) from Notification n where n.userId = :userId and n.isRead = false",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
