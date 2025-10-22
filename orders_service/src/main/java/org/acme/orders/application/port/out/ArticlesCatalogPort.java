package org.acme.orders.application.port.out;

import java.util.Optional;

public interface ArticlesCatalogPort {

    Optional<ArticleDetails> findArticleById(Long articleId);
}
