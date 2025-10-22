package org.acme.notifications.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.notifications.application.exception.NotificationApplicationException;
import org.acme.notifications.domain.model.Notification;
import org.acme.notifications.domain.model.NotificationType;
import org.acme.notifications.domain.repository.NotificationRepository;
import org.acme.notifications.interfaces.rest.dto.NotificationResponse;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Inject
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    @Override
    public void createOrderCreatedNotification(Long userId, Long orderId, String totalAmount, int itemCount) {
        Notification notification = newNotification(
                userId,
                NotificationType.ORDER_CREATED,
                "Order Created",
                String.format("Your order #%d has been created successfully with %d items (Total: %s €)",
                        orderId, itemCount, totalAmount),
                orderId
        );
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_CREATED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    @Override
    public void createOrderConfirmedNotification(Long userId, Long orderId) {
        Notification notification = newNotification(
                userId,
                NotificationType.ORDER_CONFIRMED,
                "Order Confirmed",
                String.format("Your order #%d has been confirmed and is being processed", orderId),
                orderId
        );
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_CONFIRMED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    @Override
    public void createOrderShippedNotification(Long userId, Long orderId) {
        Notification notification = newNotification(
                userId,
                NotificationType.ORDER_SHIPPED,
                "Order Shipped",
                String.format("Your order #%d has been shipped and is on its way!", orderId),
                orderId
        );
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_SHIPPED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    @Override
    public void createOrderDeliveredNotification(Long userId, Long orderId) {
        Notification notification = newNotification(
                userId,
                NotificationType.ORDER_DELIVERED,
                "Order Delivered",
                String.format("Your order #%d has been delivered successfully. Enjoy your purchase!", orderId),
                orderId
        );
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_DELIVERED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    @Override
    public void createOrderCancelledNotification(Long userId, Long orderId, String reason) {
        Notification notification = newNotification(
                userId,
                NotificationType.ORDER_CANCELLED,
                "Order Cancelled",
                String.format("Your order #%d has been cancelled. Reason: %s", orderId, reason),
                orderId
        );
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_CANCELLED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    @Override
    public void createLowStockNotification(Long articleId, String articleName, int currentStock) {
        Notification notification = newNotification(
                1L,
                NotificationType.STOCK_LOW,
                "Low Stock Alert",
                String.format("Article '%s' is running low on stock. Current stock: %d units",
                        articleName, currentStock),
                articleId
        );
        notificationRepository.persist(notification);
        LOG.infof("Created STOCK_LOW notification for article %d (%s)", articleId, articleName);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    @Override
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findOptionalById(notificationId)
                .orElseThrow(() -> new NotificationApplicationException("Notification not found", 404));

        if (!notification.getUserId().equals(userId)) {
            throw new NotificationApplicationException("Notification not found", 404);
        }

        notification.setIsRead(true);
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.findUnreadByUserId(userId)
                .forEach(notification -> notification.setIsRead(true));
        LOG.infof("Marked unread notifications as read for user %d", userId);
    }

    private Notification newNotification(Long userId, NotificationType type, String title, String message, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setRelatedEntityId(relatedId);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
