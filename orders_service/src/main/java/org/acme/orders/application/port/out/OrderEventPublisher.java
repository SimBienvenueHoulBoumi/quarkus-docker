package org.acme.orders.application.port.out;

public interface OrderEventPublisher {

    void publishOrderCreated(Long orderId, Long userId, String totalAmount, int itemCount);

    void publishOrderConfirmed(Long orderId, Long userId);

    void publishOrderShipped(Long orderId, Long userId);

    void publishOrderDelivered(Long orderId, Long userId);

    void publishOrderCancelled(Long orderId, Long userId, String reason);
}
