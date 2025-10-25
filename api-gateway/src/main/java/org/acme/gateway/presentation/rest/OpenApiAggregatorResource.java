package org.acme.gateway.presentation.rest;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Path("/openapi")
public class OpenApiAggregatorResource {

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "services.users.url", defaultValue = "http://localhost:8081")
    String usersServiceUrl;

    @ConfigProperty(name = "services.users.openapi-path", defaultValue = "/openapi/users")
    String usersOpenApiPath;

    @ConfigProperty(name = "services.articles.url", defaultValue = "http://localhost:8082")
    String articlesServiceUrl;

    @ConfigProperty(name = "services.articles.openapi-path", defaultValue = "/openapi/articles")
    String articlesOpenApiPath;

    @ConfigProperty(name = "services.orders.url", defaultValue = "http://localhost:8083")
    String ordersServiceUrl;

    @ConfigProperty(name = "services.orders.openapi-path", defaultValue = "/openapi/orders")
    String ordersOpenApiPath;

    @ConfigProperty(name = "services.notifications.url", defaultValue = "http://localhost:8084")
    String notificationsServiceUrl;

    @ConfigProperty(name = "services.notifications.openapi-path", defaultValue = "/openapi/notifications")
    String notificationsOpenApiPath;

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersOpenApi() {
        return fetchOpenApi(usersServiceUrl, usersOpenApiPath);
    }

    @GET
    @Path("/articles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArticlesOpenApi() {
        return fetchOpenApi(articlesServiceUrl, articlesOpenApiPath);
    }

    @GET
    @Path("/orders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrdersOpenApi() {
        return fetchOpenApi(ordersServiceUrl, ordersOpenApiPath);
    }

    @GET
    @Path("/notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotificationsOpenApi() {
        return fetchOpenApi(notificationsServiceUrl, notificationsOpenApiPath);
    }

    private Response fetchOpenApi(String serviceUrl, String openApiPath) {
        try {
            // Manual URL parsing to handle underscores in hostnames (Docker service names)
            String cleanUrl = serviceUrl.trim();
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "http://" + cleanUrl;
            }
            
            // Remove protocol
            String urlWithoutProtocol = cleanUrl.replaceFirst("^https?://", "");
            
            // Split host:port
            String host;
            int port;
            
            if (urlWithoutProtocol.contains(":")) {
                String[] parts = urlWithoutProtocol.split(":", 2);
                host = parts[0];
                port = Integer.parseInt(parts[1].replaceAll("/.*$", "")); // Remove any path
            } else {
                host = urlWithoutProtocol.replaceAll("/.*$", ""); // Remove any path
                port = 80;
            }

            String path = (openApiPath == null || openApiPath.isBlank()) ? "/openapi" : openApiPath.trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid service URL: " + serviceUrl);
            }
            
            // Create WebClient
            WebClient client = WebClient.create(vertx);
            
            // Make async request and wait for result
            CompletableFuture<String> future = new CompletableFuture<>();
            
            client.get(port, host, path)
                    .putHeader("Accept", MediaType.APPLICATION_JSON)
                    .send()
                    .onSuccess(response -> {
                        if (response.statusCode() == 200) {
                            future.complete(response.bodyAsString());
                        } else {
                            future.completeExceptionally(
                                new RuntimeException("Service returned status " + response.statusCode())
                            );
                        }
                    })
                    .onFailure(future::completeExceptionally);
            
            // Wait for response (with timeout)
            String body = future.get(10, TimeUnit.SECONDS);
            
            return Response.ok(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
                    
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\": \"Service unavailable: " + e.getMessage() + "\", \"url\": \"" + serviceUrl + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
