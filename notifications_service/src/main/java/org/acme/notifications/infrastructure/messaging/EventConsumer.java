package org.acme.notifications.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notifications.application.NotificationService;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventConsumer {

    private static final Logger LOG = Logger.getLogger(EventConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    NotificationService notificationService;

    @Incoming("order-events")
    @Blocking
    public void consumeOrderEvent(String message) {
        try {
            LOG.infof("Received order event: %s", message);
            JsonNode event = objectMapper.readTree(message);
            
            String eventType = event.get("eventType").asText();
            Long orderId = event.get("orderId").asLong();
            Long userId = event.get("userId").asLong();

            switch (eventType) {
                case "ORDER_CREATED":
                    String totalAmount = event.has("totalAmount") ? event.get("totalAmount").asText() : "0";
                    int itemCount = event.has("itemCount") ? event.get("itemCount").asInt() : 0;
                    notificationService.createOrderCreatedNotification(userId, orderId, totalAmount, itemCount);
                    break;
                    
                case "ORDER_CONFIRMED":
                    notificationService.createOrderConfirmedNotification(userId, orderId);
                    break;
                    
                case "ORDER_SHIPPED":
                    notificationService.createOrderShippedNotification(userId, orderId);
                    break;
                    
                case "ORDER_DELIVERED":
                    notificationService.createOrderDeliveredNotification(userId, orderId);
                    break;
                    
                case "ORDER_CANCELLED":
                    String reason = event.has("reason") ? event.get("reason").asText() : "Unknown reason";
                    notificationService.createOrderCancelledNotification(userId, orderId, reason);
                    break;
                    
                default:
                    LOG.warnf("Unknown order event type: %s", eventType);
            }
        } catch (Exception e) {
            LOG.errorf("Error processing order event: %s", e.getMessage());
        }
    }

    @Incoming("article-events")
    @Blocking
    public void consumeArticleEvent(String message) {
        try {
            LOG.infof("Received article event: %s", message);
            JsonNode event = objectMapper.readTree(message);
            
            String eventType = event.get("eventType").asText();
            Long articleId = event.get("articleId").asLong();
            String articleName = event.get("articleName").asText();

            switch (eventType) {
                case "ARTICLE_CREATED":
                    // For admin notifications - could be enhanced to notify specific users
                    LOG.infof("Article created: %s (ID: %d)", articleName, articleId);
                    break;
                    
                case "ARTICLE_UPDATED":
                    LOG.infof("Article updated: %s (ID: %d)", articleName, articleId);
                    break;
                    
                case "STOCK_CHANGED":
                    int newStock = event.get("newStock").asInt();
                    int oldStock = event.has("oldStock") ? event.get("oldStock").asInt() : 0;
                    LOG.infof("Stock changed for %s: %d -> %d", articleName, oldStock, newStock);
                    
                    // Notify admins if stock is low
                    if (newStock < 10) {
                        notificationService.createLowStockNotification(articleId, articleName, newStock);
                    }
                    break;
                    
                default:
                    LOG.warnf("Unknown article event type: %s", eventType);
            }
        } catch (Exception e) {
            LOG.errorf("Error processing article event: %s", e.getMessage());
        }
    }
}
