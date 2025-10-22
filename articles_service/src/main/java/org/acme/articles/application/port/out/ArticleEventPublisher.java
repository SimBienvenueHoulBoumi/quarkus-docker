package org.acme.articles.application.port.out;

public interface ArticleEventPublisher {

    void publishArticleCreated(Long articleId, String articleName, int stock);

    void publishArticleUpdated(Long articleId, String articleName, int stock);

    void publishArticleDeleted(Long articleId, String articleName);

    void publishStockChanged(Long articleId, String articleName, int oldStock, int newStock);

    void publishStockLow(Long articleId, String articleName, int stock);
}
