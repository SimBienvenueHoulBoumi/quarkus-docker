package org.acme.articles.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.articles.domain.model.Article;
import org.acme.articles.application.dto.response.ArticleResponse;

@ApplicationScoped
public class ArticleMapper {

    public ArticleResponse toResponse(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getName(),
                article.getDescription(),
                article.getPrice().toBigDecimal(),
                article.getStock(),
                article.getCategory(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
