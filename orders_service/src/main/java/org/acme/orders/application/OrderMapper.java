package org.acme.orders.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.model.OrderItem;
import org.acme.orders.application.dto.response.OrderItemResponse;
import org.acme.orders.application.dto.response.OrderResponse;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount().toBigDecimal(),
                order.getStatus(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getArticleId(),
                item.getArticleName(),
                item.getQuantity(),
                item.getUnitPrice().toBigDecimal(),
                item.getSubtotal().toBigDecimal()
        );
    }
}
