package org.acme.articles.application;

import org.acme.articles.interfaces.rest.dto.ArticleRequest;
import org.acme.articles.interfaces.rest.dto.ArticleResponse;

import java.util.List;

public interface ArticleService {

    List<ArticleResponse> findAll();

    List<ArticleResponse> findAvailable();

    List<ArticleResponse> findByCategory(String category);

    ArticleResponse findById(Long id);

    ArticleResponse create(ArticleRequest request);

    ArticleResponse update(Long id, ArticleRequest request);

    ArticleResponse updateStock(Long id, Integer newStock);

    void delete(Long id);
}
