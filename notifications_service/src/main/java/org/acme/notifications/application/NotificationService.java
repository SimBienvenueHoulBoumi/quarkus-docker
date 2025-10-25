package org.acme.notifications.application;

import org.acme.notifications.application.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    void createOrderCreatedNotification(Long userId, Long orderId, String totalAmount, int itemCount);

    void createOrderConfirmedNotification(Long userId, Long orderId);

    void createOrderShippedNotification(Long userId, Long orderId);

    void createOrderDeliveredNotification(Long userId, Long orderId);

    void createOrderCancelledNotification(Long userId, Long orderId, String reason);

    void createLowStockNotification(Long articleId, String articleName, int currentStock);

    List<NotificationResponse> getUserNotifications(Long userId);

    List<NotificationResponse> getUnreadNotifications(Long userId);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
