package org.acme.orders.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.acme.orders.client.ArticlesServiceClient;
import org.acme.orders.client.dto.ArticleDto;
import org.acme.orders.domain.Order;
import org.acme.orders.domain.OrderItem;
import org.acme.orders.domain.OrderStatus;
import org.acme.orders.kafka.OrderEventProducer;
import org.acme.orders.repository.OrderRepository;
import org.acme.orders.web.dto.CreateOrderRequest;
import org.acme.orders.web.dto.OrderItemRequest;
import org.acme.orders.web.dto.OrderItemResponse;
import org.acme.orders.web.dto.OrderResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    OrderRepository orderRepository;

    @Inject
    @RestClient
    ArticlesServiceClient articlesServiceClient;

    @Inject
    OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        LOG.infof("Creating order for user %d with %d items", userId, request.getItems().size());

        Order order = new Order();
        order.setUserId(userId);

        // Process each item
        for (OrderItemRequest itemRequest : request.getItems()) {
            try {
                // Fetch article details from articles_service
                ArticleDto article = articlesServiceClient.getArticleById(itemRequest.getArticleId());
                
                // Check stock availability
                if (article.getStock() < itemRequest.getQuantity()) {
                    throw new WebApplicationException(
                        String.format("Insufficient stock for article %s. Available: %d, Requested: %d",
                            article.getName(), article.getStock(), itemRequest.getQuantity()),
                        Response.Status.BAD_REQUEST
                    );
                }

                // Create order item
                OrderItem orderItem = new OrderItem();
                orderItem.setArticleId(article.getId());
                orderItem.setArticleName(article.getName());
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setUnitPrice(article.getPrice());
                
                order.addItem(orderItem);
                
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() == 404) {
                    throw new NotFoundException("Article with ID " + itemRequest.getArticleId() + " not found");
                }
                throw e;
            }
        }

        orderRepository.persist(order);
        
        // Send Kafka event
        orderEventProducer.sendOrderCreated(
            order.getId(), 
            userId, 
            order.getTotalAmount().toString(),
            order.getItems().size()
        );

        LOG.infof("Order %d created successfully for user %d", order.getId(), userId);
        return toResponse(order);
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Long userId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.persist(order);

        // Send appropriate Kafka event
        switch (newStatus) {
            case CONFIRMED:
                orderEventProducer.sendOrderConfirmed(orderId, userId);
                break;
            case SHIPPED:
                orderEventProducer.sendOrderShipped(orderId, userId);
                break;
            case DELIVERED:
                orderEventProducer.sendOrderDelivered(orderId, userId);
                break;
            case CANCELLED:
                orderEventProducer.sendOrderCancelled(orderId, userId, "Cancelled by user");
                break;
        }

        LOG.infof("Order %d status updated from %s to %s", orderId, oldStatus, newStatus);
        return toResponse(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new WebApplicationException("Cannot cancel a delivered order", Response.Status.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.persist(order);

        orderEventProducer.sendOrderCancelled(orderId, userId, "Cancelled by user");
        LOG.infof("Order %d cancelled by user %d", orderId, userId);
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    item.getArticleId(),
                    item.getArticleName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getStatus(),
            itemResponses,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
