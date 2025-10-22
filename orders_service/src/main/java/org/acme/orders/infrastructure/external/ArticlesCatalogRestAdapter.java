package org.acme.orders.infrastructure.external;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.orders.application.port.out.ArticleDetails;
import org.acme.orders.application.port.out.ArticlesCatalogPort;
import org.acme.orders.infrastructure.external.dto.ArticleDto;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Optional;

@ApplicationScoped
public class ArticlesCatalogRestAdapter implements ArticlesCatalogPort {

    private final ArticlesServiceClient articlesServiceClient;

    @Inject
    public ArticlesCatalogRestAdapter(@RestClient ArticlesServiceClient articlesServiceClient) {
        this.articlesServiceClient = articlesServiceClient;
    }

    @Override
    public Optional<ArticleDetails> findArticleById(Long articleId) {
        try {
            ArticleDto dto = articlesServiceClient.getArticleById(articleId);
            return Optional.of(new ArticleDetails(dto.getId(), dto.getName(), dto.getPrice(), dto.getStock()));
        } catch (NotFoundException notFoundException) {
            return Optional.empty();
        }
    }
}
