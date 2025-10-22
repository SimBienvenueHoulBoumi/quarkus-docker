package org.acme.articles.infrastructure.messaging;

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.articles.application.port.out.ArticleEventPublisher;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class KafkaArticleEventPublisher implements ArticleEventPublisher {

    private static final Logger LOG = Logger.getLogger(KafkaArticleEventPublisher.class);

    @Inject
    @Channel("article-events")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void publishArticleCreated(Long articleId, String articleName, int stock) {
        sendEvent("ARTICLE_CREATED", articleId, articleName, stock, null);
    }

    @Override
    public void publishArticleUpdated(Long articleId, String articleName, int stock) {
        sendEvent("ARTICLE_UPDATED", articleId, articleName, stock, null);
    }

    @Override
    public void publishArticleDeleted(Long articleId, String articleName) {
        sendEvent("ARTICLE_DELETED", articleId, articleName, null, null);
    }

    @Override
    public void publishStockChanged(Long articleId, String articleName, int oldStock, int newStock) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("oldStock", oldStock);
        additionalData.put("newStock", newStock);
        sendEvent("STOCK_CHANGED", articleId, articleName, newStock, additionalData);
    }

    @Override
    public void publishStockLow(Long articleId, String articleName, int stock) {
        sendEvent("STOCK_LOW", articleId, articleName, stock, null);
    }

    private void sendEvent(String eventType, Long articleId, String articleName, 
                          Integer stock, Map<String, Object> additionalData) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("articleId", articleId);
            event.put("articleName", articleName);
            event.put("stock", stock);
            event.put("timestamp", System.currentTimeMillis());
            
            if (additionalData != null) {
                event.putAll(additionalData);
            }

            String payload = objectMapper.writeValueAsString(event);
            
            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(articleId.toString())
                    .build();

            Message<String> message = Message.of(payload)
                    .addMetadata(metadata);

            emitter.send(message);
            
            LOG.infof("Sent Kafka event: %s for article %d", eventType, articleId);
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to serialize event: %s", e.getMessage());
        }
    }
}
