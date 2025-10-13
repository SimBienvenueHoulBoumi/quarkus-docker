package org.acme.orders.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.orders.client.dto.ArticleDto;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/articles")
@RegisterRestClient(configKey = "articles-service")
@Produces(MediaType.APPLICATION_JSON)
public interface ArticlesServiceClient {

    @GET
    @Path("/{id}")
    ArticleDto getArticleById(@PathParam("id") Long id);
}
