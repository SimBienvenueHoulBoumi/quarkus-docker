package org.acme.articles.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.articles.domain.Article;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ArticleRepository implements PanacheRepository<Article> {

    public List<Article> findByCategory(String category) {
        return list("category", category);
    }

    public List<Article> findAvailableArticles() {
        return list("stock > 0");
    }

    public List<Article> findLowStockArticles(int threshold) {
        return list("stock <= ?1 and stock > 0", threshold);
    }

    public Optional<Article> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public boolean existsByName(String name) {
        return count("LOWER(name) = LOWER(?1)", name) > 0;
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return count("LOWER(name) = LOWER(?1) and id != ?2", name, id) > 0;
    }
}
