package org.acme.notifications.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.notifications.domain.model.Notification;
import org.acme.notifications.application.dto.response.NotificationResponse;

@ApplicationScoped
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getRelatedEntityId(),
                notification.getCreatedAt()
        );
    }
}
