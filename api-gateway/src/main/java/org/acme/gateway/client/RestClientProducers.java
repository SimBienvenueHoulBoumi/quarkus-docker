package org.acme.gateway.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Produces REST client proxies with base URLs configured via application properties.
 * <p>
 * We rely on {@link URL} instead of {@link java.net.URI} so Docker service names with underscores remain valid.
 * </p>
 */
@ApplicationScoped
public class RestClientProducers {

    private static final Logger LOGGER = Logger.getLogger(RestClientProducers.class);

    @ConfigProperty(name = "services.users.url")
    String usersServiceUrl;

    @ConfigProperty(name = "services.articles.url")
    String articlesServiceUrl;

    @ConfigProperty(name = "services.orders.url")
    String ordersServiceUrl;

    @ConfigProperty(name = "services.notifications.url")
    String notificationsServiceUrl;

    @Produces
    @Singleton
    @RestClient
    UsersServiceClient usersServiceClient() {
        URL baseUrl = toUrl(usersServiceUrl, "users");
        LOGGER.infov("Configuring UsersServiceClient with base URL {0}", baseUrl);
        return RestClientBuilder.newBuilder()
                .baseUrl(baseUrl)
                .build(UsersServiceClient.class);
    }

    @Produces
    @Singleton
    @RestClient
    ArticlesServiceClient articlesServiceClient() {
        URL baseUrl = toUrl(articlesServiceUrl, "articles");
        LOGGER.infov("Configuring ArticlesServiceClient with base URL {0}", baseUrl);
        return RestClientBuilder.newBuilder()
                .baseUrl(baseUrl)
                .build(ArticlesServiceClient.class);
    }

    @Produces
    @Singleton
    @RestClient
    OrdersServiceClient ordersServiceClient() {
        URL baseUrl = toUrl(ordersServiceUrl, "orders");
        LOGGER.infov("Configuring OrdersServiceClient with base URL {0}", baseUrl);
        return RestClientBuilder.newBuilder()
                .baseUrl(baseUrl)
                .build(OrdersServiceClient.class);
    }

    @Produces
    @Singleton
    @RestClient
    NotificationsServiceClient notificationsServiceClient() {
        URL baseUrl = toUrl(notificationsServiceUrl, "notifications");
        LOGGER.infov("Configuring NotificationsServiceClient with base URL {0}", baseUrl);
        return RestClientBuilder.newBuilder()
                .baseUrl(baseUrl)
                .build(NotificationsServiceClient.class);
    }

    private static URL toUrl(String rawUrl, String serviceName) {
        String candidate = rawUrl == null ? "" : rawUrl.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("Missing base URL for " + serviceName + " service");
        }
        if (!candidate.matches("^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*$")) {
            candidate = "http://" + candidate;
        }
        try {
            return new URL(candidate);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid base URL '" + rawUrl + "' for " + serviceName + " service", e);
        }
    }
}
