package org.acme.orders.domain.event;

public enum OrderDomainEventType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED
}
