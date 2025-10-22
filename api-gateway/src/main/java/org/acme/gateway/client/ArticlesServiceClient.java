package org.acme.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/articles")
public interface ArticlesServiceClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Object getAllArticles(@QueryParam("page") Integer page, @QueryParam("size") Integer size);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Object getArticleById(@PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Object createArticle(@HeaderParam("Authorization") String authorization, Object request);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Object updateArticle(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id, Object request);

    @PATCH
    @Path("/{id}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Object updateStock(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id, Object request);

    @DELETE
    @Path("/{id}")
    Object deleteArticle(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id);
}
