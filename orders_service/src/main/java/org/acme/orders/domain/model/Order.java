package org.acme.orders.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.acme.orders.domain.event.OrderDomainEvent;
import org.acme.orders.domain.exception.OrderDomainException;
import org.acme.orders.domain.value.Money;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false, precision = 10, scale = 2))
    })
    private Money totalAmount = Money.zero();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    private final List<OrderDomainEvent> domainEvents = new ArrayList<>();

    public static Order create(Long userId) {
        Objects.requireNonNull(userId, "userId");
        Order order = new Order();
        order.userId = userId;
        order.status = OrderStatus.PENDING;
        order.totalAmount = Money.zero();
        Instant now = Instant.now();
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            Instant now = Instant.now();
            this.createdAt = now;
            this.updatedAt = now;
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (totalAmount == null) {
            totalAmount = Money.zero();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "item");
        item.setOrder(this);
        items.add(item);
        recalculateTotal();
    }

    public void confirm() {
        ensureState(OrderStatus.PENDING, "Only pending orders can be confirmed");
        status = OrderStatus.CONFIRMED;
        updatedAt = Instant.now();
        registerEvent(OrderDomainEvent.confirmed(this));
    }

    public void ship() {
        ensureState(OrderStatus.CONFIRMED, "Only confirmed orders can be shipped");
        status = OrderStatus.SHIPPED;
        updatedAt = Instant.now();
        registerEvent(OrderDomainEvent.shipped(this));
    }

    public void deliver() {
        ensureState(OrderStatus.SHIPPED, "Only shipped orders can be delivered");
        status = OrderStatus.DELIVERED;
        updatedAt = Instant.now();
        registerEvent(OrderDomainEvent.delivered(this));
    }

    public void cancel(String reason) {
        if (status == OrderStatus.DELIVERED) {
            throw new OrderDomainException("A delivered order cannot be cancelled");
        }
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now();
        registerEvent(OrderDomainEvent.cancelled(this, reason));
    }

    private void ensureState(OrderStatus expected, String message) {
        if (status != expected) {
            throw new OrderDomainException(message);
        }
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money.zero(), Money::add);
    }

    public void registerEvent(OrderDomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    public List<OrderDomainEvent> pullDomainEvents() {
        List<OrderDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Money totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
