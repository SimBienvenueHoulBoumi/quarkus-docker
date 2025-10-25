package org.acme.orders.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.orders.application.exception.OrderApplicationException;
import org.acme.orders.application.port.out.ArticleDetails;
import org.acme.orders.application.port.out.ArticlesCatalogPort;
import org.acme.orders.application.port.out.OrderEventPublisher;
import org.acme.orders.domain.event.OrderDomainEvent;
import org.acme.orders.domain.exception.OrderDomainException;
import org.acme.orders.domain.model.Order;
import org.acme.orders.domain.model.OrderStatus;
import org.acme.orders.domain.repository.OrderRepository;
import org.acme.orders.domain.service.OrderDomainService;
import org.acme.orders.application.dto.request.CreateOrderRequest;
import org.acme.orders.application.dto.request.OrderItemRequest;
import org.acme.orders.application.dto.response.OrderResponse;

import java.util.List;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ArticlesCatalogPort articlesCatalogPort;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderMapper orderMapper;
    private final OrderDomainService orderDomainService;

    @Inject
    public OrderServiceImpl(OrderRepository orderRepository,
                            ArticlesCatalogPort articlesCatalogPort,
                            OrderEventPublisher orderEventPublisher,
                            OrderMapper orderMapper,
                            OrderDomainService orderDomainService) {
        this.orderRepository = orderRepository;
        this.articlesCatalogPort = articlesCatalogPort;
        this.orderEventPublisher = orderEventPublisher;
        this.orderMapper = orderMapper;
        this.orderDomainService = orderDomainService;
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
            try {
                orderDomainService.addItem(order, article, itemRequest.getQuantity());
            } catch (OrderDomainException ex) {
                throw new OrderApplicationException(ex.getMessage(), 400);
            }
        }

        orderRepository.persist(order);
        orderDomainService.registerCreationEvent(order);
        publishDomainEvents(order);

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
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findOptionalById(orderId)
                .orElseThrow(() -> new OrderApplicationException("Order not found", 404));

        try {
            if (newStatus == OrderStatus.CANCELLED) {
                orderDomainService.cancelOrder(order, "Cancelled by admin");
            } else {
                orderDomainService.changeStatus(order, newStatus);
            }
        } catch (OrderDomainException ex) {
            throw new OrderApplicationException(ex.getMessage(), 400);
        }

        orderRepository.persist(order);
        publishDomainEvents(order);

        return orderMapper.toResponse(order);
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderApplicationException("Order not found", 404));

        try {
            orderDomainService.cancelOrder(order, "Cancelled by user");
        } catch (OrderDomainException ex) {
            throw new OrderApplicationException(ex.getMessage(), 400);
        }

        orderRepository.persist(order);
        publishDomainEvents(order);
    }

    private void publishDomainEvents(Order order) {
        for (OrderDomainEvent event : order.pullDomainEvents()) {
            switch (event.type()) {
                case ORDER_CREATED -> orderEventPublisher.publishOrderCreated(
                        event.orderId(),
                        event.userId(),
                        event.totalAmount().toPlainString(),
                        event.itemCount());
                case ORDER_CONFIRMED -> orderEventPublisher.publishOrderConfirmed(event.orderId(), event.userId());
                case ORDER_SHIPPED -> orderEventPublisher.publishOrderShipped(event.orderId(), event.userId());
                case ORDER_DELIVERED -> orderEventPublisher.publishOrderDelivered(event.orderId(), event.userId());
                case ORDER_CANCELLED -> orderEventPublisher.publishOrderCancelled(
                        event.orderId(),
                        event.userId(),
                        event.reason() != null ? event.reason() : "Cancelled");
            }
        }
    }
}
