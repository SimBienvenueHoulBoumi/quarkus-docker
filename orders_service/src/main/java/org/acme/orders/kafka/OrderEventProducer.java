package org.acme.orders.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class OrderEventProducer {

    private static final Logger LOG = Logger.getLogger(OrderEventProducer.class);

    @Inject
    @Channel("order-events")
    Emitter<String> orderEventsEmitter;

    @Inject
    ObjectMapper objectMapper;

    public void sendOrderCreated(Long orderId, Long userId, String totalAmount, int itemCount) {
        sendEvent("ORDER_CREATED", orderId, userId, totalAmount, itemCount, null);
    }

    public void sendOrderConfirmed(Long orderId, Long userId) {
        sendEvent("ORDER_CONFIRMED", orderId, userId, null, null, null);
    }

    public void sendOrderShipped(Long orderId, Long userId) {
        sendEvent("ORDER_SHIPPED", orderId, userId, null, null, null);
    }

    public void sendOrderDelivered(Long orderId, Long userId) {
        sendEvent("ORDER_DELIVERED", orderId, userId, null, null, null);
    }

    public void sendOrderCancelled(Long orderId, Long userId, String reason) {
        sendEvent("ORDER_CANCELLED", orderId, userId, null, null, reason);
    }

    private void sendEvent(String eventType, Long orderId, Long userId, 
                          String totalAmount, Integer itemCount, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("orderId", orderId);
            event.put("userId", userId);
            event.put("timestamp", System.currentTimeMillis());
            
            if (totalAmount != null) {
                event.put("totalAmount", totalAmount);
            }
            if (itemCount != null) {
                event.put("itemCount", itemCount);
            }
            if (reason != null) {
                event.put("reason", reason);
            }

            String payload = objectMapper.writeValueAsString(event);
            
            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(orderId.toString())
                    .build();

            Message<String> message = Message.of(payload).addMetadata(metadata);
            orderEventsEmitter.send(message);
            
            LOG.infof("Order event sent: %s for order %d", eventType, orderId);
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to serialize order event: %s", e.getMessage());
        }
    }
}
