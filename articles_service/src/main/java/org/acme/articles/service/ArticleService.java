package org.acme.articles.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.acme.articles.domain.Article;
import org.acme.articles.kafka.ArticleEventProducer;
import org.acme.articles.repository.ArticleRepository;
import org.acme.articles.web.dto.ArticleRequest;
import org.acme.articles.web.dto.ArticleResponse;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ArticleService {

    private static final Logger LOG = Logger.getLogger(ArticleService.class);
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Inject
    ArticleRepository articleRepository;

    @Inject
    ArticleEventProducer eventProducer;

    public List<ArticleResponse> findAll() {
        return articleRepository.listAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> findAvailable() {
        return articleRepository.findAvailableArticles().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> findByCategory(String category) {
        return articleRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ArticleResponse findById(Long id) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Article not found with id: " + id));
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request) {
        // Check if article with same name already exists
        if (articleRepository.existsByName(request.getName())) {
            throw new WebApplicationException(
                "Article with name '" + request.getName() + "' already exists",
                Response.Status.CONFLICT
            );
        }

        Article article = new Article();
        article.setName(request.getName());
        article.setDescription(request.getDescription());
        article.setPrice(request.getPrice());
        article.setStock(request.getStock());
        article.setCategory(request.getCategory());

        articleRepository.persist(article);
        LOG.infof("Created article: %s (ID: %d)", article.getName(), article.getId());

        // Send Kafka event
        eventProducer.sendArticleCreated(article.getId(), article.getName(), article.getStock());

        // Check if stock is low
        if (article.getStock() <= LOW_STOCK_THRESHOLD) {
            eventProducer.sendStockLow(article.getId(), article.getName(), article.getStock());
        }

        return toResponse(article);
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Article not found with id: " + id));

        // Check if another article with same name exists
        if (articleRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new WebApplicationException(
                "Another article with name '" + request.getName() + "' already exists",
                Response.Status.CONFLICT
            );
        }

        Integer oldStock = article.getStock();

        article.setName(request.getName());
        article.setDescription(request.getDescription());
        article.setPrice(request.getPrice());
        article.setStock(request.getStock());
        article.setCategory(request.getCategory());

        articleRepository.persist(article);
        LOG.infof("Updated article: %s (ID: %d)", article.getName(), article.getId());

        // Send Kafka event
        eventProducer.sendArticleUpdated(article.getId(), article.getName(), article.getStock());

        // If stock changed, send stock changed event
        if (!oldStock.equals(article.getStock())) {
            eventProducer.sendStockChanged(article.getId(), article.getName(), oldStock, article.getStock());
            
            if (article.getStock() <= LOW_STOCK_THRESHOLD && oldStock > LOW_STOCK_THRESHOLD) {
                eventProducer.sendStockLow(article.getId(), article.getName(), article.getStock());
            }
        }

        return toResponse(article);
    }

    @Transactional
    public ArticleResponse updateStock(Long id, Integer newStock) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Article not found with id: " + id));

        Integer oldStock = article.getStock();
        article.setStock(newStock);

        articleRepository.persist(article);
        LOG.infof("Updated stock for article %s (ID: %d): %d -> %d", 
                  article.getName(), article.getId(), oldStock, newStock);

        // Send Kafka events
        eventProducer.sendStockChanged(article.getId(), article.getName(), oldStock, newStock);

        if (newStock <= LOW_STOCK_THRESHOLD && oldStock > LOW_STOCK_THRESHOLD) {
            eventProducer.sendStockLow(article.getId(), article.getName(), newStock);
        }

        return toResponse(article);
    }

    @Transactional
    public void delete(Long id) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Article not found with id: " + id));

        articleRepository.delete(article);
        LOG.infof("Deleted article: %s (ID: %d)", article.getName(), article.getId());

        // Send Kafka event
        eventProducer.sendArticleDeleted(article.getId(), article.getName());
    }

    public ArticleResponse toResponse(Article article) {
        return new ArticleResponse(
            article.getId(),
            article.getName(),
            article.getDescription(),
            article.getPrice(),
            article.getStock(),
            article.getCategory(),
            article.getCreatedAt(),
            article.getUpdatedAt()
        );
    }
}
