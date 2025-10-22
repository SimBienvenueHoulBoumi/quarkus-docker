package org.acme.articles.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.articles.domain.model.Article;
import org.acme.articles.domain.value.Money;

import java.math.BigDecimal;

@ApplicationScoped
public class ArticleDomainService {

    public Article create(String name, String description, BigDecimal price, Integer stock, String category) {
        return Article.create(name, description, Money.of(price), stock, category);
    }

    public void update(Article article, String name, String description, BigDecimal price, Integer stock, String category) {
        article.updateDetails(name, description, Money.of(price), stock, category);
    }

    public void changeStock(Article article, Integer newStock) {
        article.changeStock(newStock);
    }

    public boolean isLowStock(Article article, int threshold) {
        return article.isLowStock(threshold);
    }
}
