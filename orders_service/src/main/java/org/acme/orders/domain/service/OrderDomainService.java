package org.acme.orders.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.orders.domain.event.OrderDomainEvent;
import org.acme.orders.domain.exception.OrderDomainException;
import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.model.OrderItem;
import org.acme.orders.domain.model.OrderStatus;
import org.acme.orders.domain.value.Money;
import org.acme.orders.application.port.out.ArticleDetails;

@ApplicationScoped
public class OrderDomainService {

    public void addItem(Order order, ArticleDetails article, int quantity) {
        if (article.stock() < quantity) {
            throw new OrderDomainException(String.format(
                    "Insufficient stock for article %s. Available: %d, Requested: %d",
                    article.name(), article.stock(), quantity));
        }

        try {
            OrderItem item = OrderItem.create(
                    article.id(),
                    article.name(),
                    Money.of(article.price()),
                    quantity);
            order.addItem(item);
        } catch (IllegalArgumentException ex) {
            throw new OrderDomainException(ex.getMessage());
        }
    }

    public void registerCreationEvent(Order order) {
        order.registerEvent(OrderDomainEvent.created(order));
    }

    public void changeStatus(Order order, OrderStatus newStatus) {
        switch (newStatus) {
            case CONFIRMED -> order.confirm();
            case SHIPPED -> order.ship();
            case DELIVERED -> order.deliver();
            case CANCELLED -> throw new OrderDomainException("Use cancelOrder for cancellations");
            case PENDING -> throw new OrderDomainException("Cannot revert to PENDING once created");
        }
    }

    public void cancelOrder(Order order, String reason) {
        order.cancel(reason);
    }
}
