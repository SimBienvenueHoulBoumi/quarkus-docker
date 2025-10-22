package org.acme.orders.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.orders.application.exception.OrderApplicationException;
import org.acme.orders.application.port.out.ArticleDetails;
import org.acme.orders.application.port.out.ArticlesCatalogPort;
import org.acme.orders.application.port.out.OrderEventPublisher;
import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.model.OrderItem;
import org.acme.orders.domain.model.OrderStatus;
import org.acme.orders.domain.repository.OrderRepository;
import org.acme.orders.interfaces.rest.dto.CreateOrderRequest;
import org.acme.orders.interfaces.rest.dto.OrderItemRequest;
import org.acme.orders.interfaces.rest.dto.OrderResponse;

import java.util.List;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ArticlesCatalogPort articlesCatalogPort;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderMapper orderMapper;

    @Inject
    public OrderServiceImpl(OrderRepository orderRepository,
                            ArticlesCatalogPort articlesCatalogPort,
                            OrderEventPublisher orderEventPublisher,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.articlesCatalogPort = articlesCatalogPort;
        this.orderEventPublisher = orderEventPublisher;
        this.orderMapper = orderMapper;
    }

    @Transactional
    @Override
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderApplicationException("Order must contain at least one item", 400);
        }

        Order order = Order.create(userId);

        for (OrderItemRequest itemRequest : request.getItems()) {
            ArticleDetails article = articlesCatalogPort.findArticleById(itemRequest.getArticleId())
                    .orElseThrow(() -> new OrderApplicationException(
                            "Article with ID %d not found".formatted(itemRequest.getArticleId()), 404));

            if (article.stock() < itemRequest.getQuantity()) {
                throw new OrderApplicationException(
                        "Insufficient stock for article %s. Available: %d, Requested: %d"
                                .formatted(article.name(), article.stock(), itemRequest.getQuantity()),
                        400);
            }

            OrderItem orderItem = OrderItem.create(
                    article.id(),
                    article.name(),
                    article.price(),
                    itemRequest.getQuantity()
            );
            order.addItem(orderItem);
        }

        orderRepository.persist(order);

        orderEventPublisher.publishOrderCreated(
                order.getId(),
                userId,
                order.getTotalAmount().toString(),
                order.getItems().size()
        );

        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderApplicationException("Order not found", 404));
        return orderMapper.toResponse(order);
    }

    @Transactional
    @Override
    public OrderResponse updateOrderStatus(Long orderId, Long userId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderApplicationException("Order not found", 404));

        switch (newStatus) {
            case CONFIRMED -> order.confirm();
            case SHIPPED -> order.ship();
            case DELIVERED -> order.deliver();
            case CANCELLED -> order.cancel();
            case PENDING -> throw new OrderApplicationException("Cannot revert to PENDING once created", 400);
        }
        orderRepository.persist(order);
        publishStatusEvent(orderId, userId, newStatus);

        return orderMapper.toResponse(order);
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderApplicationException("Order not found", 404));

        order.cancel();
        orderRepository.persist(order);
        orderEventPublisher.publishOrderCancelled(orderId, userId, "Cancelled by user");
    }

    private void publishStatusEvent(Long orderId, Long userId, OrderStatus status) {
        switch (status) {
            case CONFIRMED -> orderEventPublisher.publishOrderConfirmed(orderId, userId);
            case SHIPPED -> orderEventPublisher.publishOrderShipped(orderId, userId);
            case DELIVERED -> orderEventPublisher.publishOrderDelivered(orderId, userId);
            case CANCELLED -> orderEventPublisher.publishOrderCancelled(orderId, userId, "Cancelled by user");
            default -> {
            }
        }
    }

}
