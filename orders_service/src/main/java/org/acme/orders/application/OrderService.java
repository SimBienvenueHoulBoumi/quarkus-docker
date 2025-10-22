package org.acme.orders.application;

import org.acme.orders.domain.model.OrderStatus;
import org.acme.orders.interfaces.rest.dto.CreateOrderRequest;
import org.acme.orders.interfaces.rest.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    List<OrderResponse> getUserOrders(Long userId);

    OrderResponse getOrderById(Long orderId, Long userId);

    OrderResponse updateOrderStatus(Long orderId, Long userId, OrderStatus newStatus);

    void cancelOrder(Long orderId, Long userId);
}
