package org.acme.gateway;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Path("/openapi")
public class OpenApiAggregatorResource {

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "services.users.url")
    String usersServiceUrl;

    @ConfigProperty(name = "services.articles.url")
    String articlesServiceUrl;

    @ConfigProperty(name = "services.orders.url")
    String ordersServiceUrl;

    @ConfigProperty(name = "services.notifications.url")
    String notificationsServiceUrl;

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersOpenApi() {
        return fetchOpenApi(usersServiceUrl);
    }

    @GET
    @Path("/articles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArticlesOpenApi() {
        return fetchOpenApi(articlesServiceUrl);
    }

    @GET
    @Path("/orders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrdersOpenApi() {
        return fetchOpenApi(ordersServiceUrl);
    }

    @GET
    @Path("/notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotificationsOpenApi() {
        return fetchOpenApi(notificationsServiceUrl);
    }

    private Response fetchOpenApi(String serviceUrl) {
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
            
            String path = "/q/openapi";
            
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid service URL: " + serviceUrl);
            }
            
            // Create WebClient
            WebClient client = WebClient.create(vertx);
            
            // Make async request and wait for result
            CompletableFuture<String> future = new CompletableFuture<>();
            
            client.get(port, host, path)
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
