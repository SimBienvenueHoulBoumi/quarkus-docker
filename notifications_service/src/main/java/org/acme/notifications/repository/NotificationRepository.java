package org.acme.notifications.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.notifications.domain.Notification;
import org.acme.notifications.domain.NotificationType;

import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {

    public List<Notification> findByUserId(Long userId) {
        return list("userId = ?1 order by createdAt desc", userId);
    }

    public List<Notification> findUnreadByUserId(Long userId) {
        return list("userId = ?1 and isRead = false order by createdAt desc", userId);
    }

    public List<Notification> findByUserIdAndType(Long userId, NotificationType type) {
        return list("userId = ?1 and type = ?2 order by createdAt desc", userId, type);
    }

    public long countUnreadByUserId(Long userId) {
        return count("userId = ?1 and isRead = false", userId);
    }
}
