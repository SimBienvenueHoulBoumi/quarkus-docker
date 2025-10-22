package org.acme.gateway.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.acme.gateway.client.ArticlesServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArticlesGatewayResource {

    @Inject
    @RestClient
    ArticlesServiceClient articlesServiceClient;

    @GET
    public Object getAllArticles(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        return articlesServiceClient.getAllArticles(page, size);
    }

    @GET
    @Path("/{id}")
    public Object getArticleById(@PathParam("id") Long id) {
        return articlesServiceClient.getArticleById(id);
    }

    @POST
    public Object createArticle(@Context HttpHeaders headers, Object request) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return articlesServiceClient.createArticle(authorization, request);
    }

    @PUT
    @Path("/{id}")
    public Object updateArticle(@Context HttpHeaders headers, @PathParam("id") Long id, Object request) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return articlesServiceClient.updateArticle(authorization, id, request);
    }

    @PATCH
    @Path("/{id}/stock")
    public Object updateStock(@Context HttpHeaders headers, @PathParam("id") Long id, Object request) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return articlesServiceClient.updateStock(authorization, id, request);
    }

    @DELETE
    @Path("/{id}")
    public Object deleteArticle(@Context HttpHeaders headers, @PathParam("id") Long id) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return articlesServiceClient.deleteArticle(authorization, id);
    }
}
