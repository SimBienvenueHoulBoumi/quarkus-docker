package org.acme.notifications.domain;

public enum NotificationType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ARTICLE_CREATED,
    ARTICLE_UPDATED,
    STOCK_LOW,
    STOCK_CHANGED
}
