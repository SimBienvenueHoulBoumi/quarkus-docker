package org.acme.orders.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.orders.domain.Order;
import org.acme.orders.domain.OrderStatus;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public List<Order> findByUserId(Long userId) {
        return list("userId", userId);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return list("status", status);
    }

    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return list("userId = ?1 and status = ?2", userId, status);
    }

    public Optional<Order> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public Optional<Order> findByIdAndUserId(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }
}
