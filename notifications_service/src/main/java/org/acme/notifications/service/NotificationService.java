package org.acme.notifications.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.notifications.domain.Notification;
import org.acme.notifications.domain.NotificationType;
import org.acme.notifications.repository.NotificationRepository;
import org.acme.notifications.web.dto.NotificationResponse;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    @Inject
    NotificationRepository notificationRepository;

    @Transactional
    public void createOrderCreatedNotification(Long userId, Long orderId, String totalAmount, int itemCount) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Order Created");
        notification.setMessage(String.format("Your order #%d has been created successfully with %d items (Total: %s €)", 
            orderId, itemCount, totalAmount));
        notification.setRelatedEntityId(orderId);
        
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_CREATED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    public void createOrderConfirmedNotification(Long userId, Long orderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_CONFIRMED);
        notification.setTitle("Order Confirmed");
        notification.setMessage(String.format("Your order #%d has been confirmed and is being processed", orderId));
        notification.setRelatedEntityId(orderId);
        
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_CONFIRMED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    public void createOrderShippedNotification(Long userId, Long orderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_SHIPPED);
        notification.setTitle("Order Shipped");
        notification.setMessage(String.format("Your order #%d has been shipped and is on its way!", orderId));
        notification.setRelatedEntityId(orderId);
        
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_SHIPPED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    public void createOrderDeliveredNotification(Long userId, Long orderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_DELIVERED);
        notification.setTitle("Order Delivered");
        notification.setMessage(String.format("Your order #%d has been delivered successfully. Enjoy your purchase!", orderId));
        notification.setRelatedEntityId(orderId);
        
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_DELIVERED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    public void createOrderCancelledNotification(Long userId, Long orderId, String reason) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_CANCELLED);
        notification.setTitle("Order Cancelled");
        notification.setMessage(String.format("Your order #%d has been cancelled. Reason: %s", orderId, reason));
        notification.setRelatedEntityId(orderId);
        
        notificationRepository.persist(notification);
        LOG.infof("Created ORDER_CANCELLED notification for user %d, order %d", userId, orderId);
    }

    @Transactional
    public void createLowStockNotification(Long articleId, String articleName, int currentStock) {
        // This could be sent to admin users - for now we'll use userId = 1 as admin
        Notification notification = new Notification();
        notification.setUserId(1L); // Admin user
        notification.setType(NotificationType.STOCK_LOW);
        notification.setTitle("Low Stock Alert");
        notification.setMessage(String.format("Article '%s' is running low on stock. Current stock: %d units", 
            articleName, currentStock));
        notification.setRelatedEntityId(articleId);
        
        notificationRepository.persist(notification);
        LOG.infof("Created STOCK_LOW notification for article %d (%s)", articleId, articleName);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId);
        
        if (notification == null) {
            throw new NotFoundException("Notification not found");
        }
        
        if (!notification.getUserId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }
        
        notification.setIsRead(true);
        notificationRepository.persist(notification);
        
        LOG.infof("Marked notification %d as read for user %d", notificationId, userId);
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        
        LOG.infof("Marked %d notifications as read for user %d", unreadNotifications.size(), userId);
    }

    private NotificationResponse toResponse(Notification notification) {
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
