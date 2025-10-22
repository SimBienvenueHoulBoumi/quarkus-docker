package org.acme.articles.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.acme.articles.domain.model.Article;
import org.acme.articles.domain.repository.ArticleRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ArticleJpaRepository implements ArticleRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public void persist(Article article) {
        if (article.getId() == null) {
            entityManager.persist(article);
        } else {
            entityManager.merge(article);
        }
    }

    @Override
    public void delete(Article article) {
        entityManager.remove(entityManager.contains(article) ? article : entityManager.merge(article));
    }

    @Override
    public List<Article> listAll() {
        return entityManager.createQuery("from Article", Article.class).getResultList();
    }

    @Override
    public List<Article> findAvailableArticles() {
        return entityManager.createQuery(
                        "select a from Article a where a.stock > 0",
                        Article.class)
                .getResultList();
    }

    @Override
    public List<Article> findByCategory(String category) {
        return entityManager.createQuery(
                        "select a from Article a where a.category = :category",
                        Article.class)
                .setParameter("category", category)
                .getResultList();
    }

    @Override
    public Optional<Article> findByIdOptional(Long id) {
        return Optional.ofNullable(entityManager.find(Article.class, id));
    }

    @Override
    public boolean existsByName(String name) {
        Long count = entityManager.createQuery(
                        "select count(a) from Article a where lower(a.name) = lower(:name)",
                        Long.class)
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        Long count = entityManager.createQuery(
                        "select count(a) from Article a where lower(a.name) = lower(:name) and a.id <> :id",
                        Long.class)
                .setParameter("name", name)
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }
}
