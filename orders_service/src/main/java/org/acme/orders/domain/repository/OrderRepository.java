package org.acme.orders.domain.repository;

import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void persist(Order order);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findOptionalById(Long id);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
}
