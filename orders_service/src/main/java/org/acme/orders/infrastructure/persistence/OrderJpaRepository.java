package org.acme.orders.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.model.OrderStatus;
import org.acme.orders.domain.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderJpaRepository implements OrderRepository {

    @PersistenceContext
    EntityManager entityManager;

    public void persist(Order order) {
        if (order.getId() == null) {
            entityManager.persist(order);
        } else {
            entityManager.merge(order);
        }
    }

    public Optional<Order> findByIdAndUserId(Long id, Long userId) {
        TypedQuery<Order> query = entityManager.createQuery(
                "select o from Order o where o.id = :id and o.userId = :userId",
                Order.class);
        query.setParameter("id", id);
        query.setParameter("userId", userId);
        return query.getResultStream().findFirst();
    }

    public List<Order> findByUserId(Long userId) {
        return entityManager.createQuery(
                "select o from Order o where o.userId = :userId",
                Order.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return entityManager.createQuery(
                "select o from Order o where o.userId = :userId and o.status = :status",
                Order.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList();
    }

}
