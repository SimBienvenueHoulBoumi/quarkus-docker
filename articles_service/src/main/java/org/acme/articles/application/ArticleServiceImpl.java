package org.acme.articles.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.articles.application.exception.ArticleApplicationException;
import org.acme.articles.application.port.out.ArticleEventPublisher;
import org.acme.articles.domain.model.Article;
import org.acme.articles.domain.repository.ArticleRepository;
import org.acme.articles.interfaces.rest.dto.ArticleRequest;
import org.acme.articles.interfaces.rest.dto.ArticleResponse;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ArticleServiceImpl implements ArticleService {

    private static final Logger LOG = Logger.getLogger(ArticleServiceImpl.class);
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ArticleRepository articleRepository;
    private final ArticleEventPublisher articleEventPublisher;
    private final ArticleMapper articleMapper;

    @Inject
    public ArticleServiceImpl(ArticleRepository articleRepository,
                              ArticleEventPublisher articleEventPublisher,
                              ArticleMapper articleMapper) {
        this.articleRepository = articleRepository;
        this.articleEventPublisher = articleEventPublisher;
        this.articleMapper = articleMapper;
    }

    @Override
    public List<ArticleResponse> findAll() {
        return articleRepository.listAll().stream()
                .map(articleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArticleResponse> findAvailable() {
        return articleRepository.findAvailableArticles().stream()
                .map(articleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArticleResponse> findByCategory(String category) {
        return articleRepository.findByCategory(category).stream()
                .map(articleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ArticleResponse findById(Long id) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new ArticleApplicationException("Article not found with id: " + id, 404));
        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public ArticleResponse create(ArticleRequest request) {
        if (articleRepository.existsByName(request.getName())) {
            throw new ArticleApplicationException(
                    "Article with name '" + request.getName() + "' already exists",
                    409
            );
        }

        Article article = Article.create(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getCategory()
        );

        articleRepository.persist(article);
        LOG.infof("Created article: %s (ID: %d)", article.getName(), article.getId());

        articleEventPublisher.publishArticleCreated(article.getId(), article.getName(), article.getStock());
        if (isLowStock(article.getStock())) {
            articleEventPublisher.publishStockLow(article.getId(), article.getName(), article.getStock());
        }

        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public ArticleResponse update(Long id, ArticleRequest request) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new ArticleApplicationException("Article not found with id: " + id, 404));

        if (articleRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new ArticleApplicationException(
                    "Another article with name '" + request.getName() + "' already exists",
                    409
            );
        }

        int previousStock = article.getStock();
        article.updateDetails(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getCategory()
        );

        articleRepository.persist(article);
        LOG.infof("Updated article: %s (ID: %d)", article.getName(), article.getId());

        articleEventPublisher.publishArticleUpdated(article.getId(), article.getName(), article.getStock());
        handleStockChange(article, previousStock);

        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public ArticleResponse updateStock(Long id, Integer newStock) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new ArticleApplicationException("Article not found with id: " + id, 404));

        int previousStock = article.getStock();
        article.changeStock(newStock);

        articleRepository.persist(article);
        LOG.infof("Updated stock for article %s (ID: %d): %d -> %d",
                article.getName(), article.getId(), previousStock, newStock);

        handleStockChange(article, previousStock);

        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Article article = articleRepository.findByIdOptional(id)
                .orElseThrow(() -> new ArticleApplicationException("Article not found with id: " + id, 404));

        articleRepository.delete(article);
        LOG.infof("Deleted article: %s (ID: %d)", article.getName(), article.getId());

        articleEventPublisher.publishArticleDeleted(article.getId(), article.getName());
    }

    private void handleStockChange(Article article, int previousStock) {
        int newStock = article.getStock();
        if (previousStock != newStock) {
            articleEventPublisher.publishStockChanged(article.getId(), article.getName(), previousStock, newStock);
            if (isLowStock(newStock) && previousStock > LOW_STOCK_THRESHOLD) {
                articleEventPublisher.publishStockLow(article.getId(), article.getName(), newStock);
            }
        }
    }

    private boolean isLowStock(int stock) {
        return stock <= LOW_STOCK_THRESHOLD;
    }
}
