package org.acme.notifications.domain.repository;

import org.acme.notifications.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    void persist(Notification notification);

    Optional<Notification> findOptionalById(Long id);

    List<Notification> findByUserId(Long userId);

    List<Notification> findUnreadByUserId(Long userId);

    long countUnreadByUserId(Long userId);
}
