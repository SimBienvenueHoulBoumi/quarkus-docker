package org.acme.articles.domain.repository;

import org.acme.articles.domain.model.Article;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository {

    void persist(Article article);

    void delete(Article article);

    List<Article> listAll();

    List<Article> findAvailableArticles();

    List<Article> findByCategory(String category);

    Optional<Article> findByIdOptional(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
