package org.acme.orders.domain.event;

import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.value.Money;

import java.time.Instant;

public record OrderDomainEvent(
        OrderDomainEventType type,
        Long orderId,
        Long userId,
        Money totalAmount,
        int itemCount,
        String reason,
        Instant occurredOn
) {

    public static OrderDomainEvent created(Order order) {
        return new OrderDomainEvent(
                OrderDomainEventType.ORDER_CREATED,
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getItems().size(),
                null,
                Instant.now());
    }

    public static OrderDomainEvent confirmed(Order order) {
        return statusChanged(order, OrderDomainEventType.ORDER_CONFIRMED);
    }

    public static OrderDomainEvent shipped(Order order) {
        return statusChanged(order, OrderDomainEventType.ORDER_SHIPPED);
    }

    public static OrderDomainEvent delivered(Order order) {
        return statusChanged(order, OrderDomainEventType.ORDER_DELIVERED);
    }

    public static OrderDomainEvent cancelled(Order order, String reason) {
        return new OrderDomainEvent(
                OrderDomainEventType.ORDER_CANCELLED,
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getItems().size(),
                reason,
                Instant.now());
    }

    private static OrderDomainEvent statusChanged(Order order, OrderDomainEventType type) {
        return new OrderDomainEvent(
                type,
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getItems().size(),
                null,
                Instant.now());
    }
}
