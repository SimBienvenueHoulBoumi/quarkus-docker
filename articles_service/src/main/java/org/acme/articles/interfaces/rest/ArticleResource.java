package org.acme.articles.interfaces.rest;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.articles.application.ArticleService;
import org.acme.articles.application.exception.ArticleApplicationException;
import org.acme.articles.interfaces.rest.dto.ArticleRequest;
import org.acme.articles.interfaces.rest.dto.ArticleResponse;
import org.acme.articles.interfaces.rest.dto.StockUpdateRequest;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.function.Supplier;

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Articles", description = "Article management endpoints")
public class ArticleResource {

    @Inject
    ArticleService articleService;

    @GET
    @PermitAll
    @Operation(summary = "Get all articles", description = "Returns a list of all articles")
    @APIResponse(responseCode = "200", description = "List of articles")
    public List<ArticleResponse> getAllArticles() {
        return execute(articleService::findAll);
    }

    @GET
    @Path("/available")
    @PermitAll
    @Operation(summary = "Get available articles", description = "Returns articles with stock > 0")
    @APIResponse(responseCode = "200", description = "List of available articles")
    public List<ArticleResponse> getAvailableArticles() {
        return execute(articleService::findAvailable);
    }

    @GET
    @Path("/category/{category}")
    @PermitAll
    @Operation(summary = "Get articles by category", description = "Returns articles filtered by category")
    @APIResponse(responseCode = "200", description = "List of articles in category")
    public List<ArticleResponse> getArticlesByCategory(@PathParam("category") String category) {
        return execute(() -> articleService.findByCategory(category));
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Get article by ID", description = "Returns a single article by its ID")
    @APIResponse(responseCode = "200", description = "Article found")
    @APIResponse(responseCode = "404", description = "Article not found")
    public ArticleResponse getArticleById(@PathParam("id") Long id) {
        return execute(() -> articleService.findById(id));
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create article", description = "Creates a new article (ADMIN only)")
    @APIResponse(responseCode = "201", description = "Article created")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden - Admin role required")
    @APIResponse(responseCode = "409", description = "Article with same name already exists")
    public Response createArticle(@Valid ArticleRequest request) {
        ArticleResponse response = execute(() -> articleService.create(request));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update article", description = "Updates an existing article (ADMIN only)")
    @APIResponse(responseCode = "200", description = "Article updated")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden - Admin role required")
    @APIResponse(responseCode = "404", description = "Article not found")
    @APIResponse(responseCode = "409", description = "Another article with same name exists")
    public ArticleResponse updateArticle(@PathParam("id") Long id, @Valid ArticleRequest request) {
        return execute(() -> articleService.update(id, request));
    }

    @PATCH
    @Path("/{id}/stock")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update article stock", description = "Updates only the stock of an article (ADMIN only)")
    @APIResponse(responseCode = "200", description = "Stock updated")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden - Admin role required")
    @APIResponse(responseCode = "404", description = "Article not found")
    public ArticleResponse updateStock(@PathParam("id") Long id, @Valid StockUpdateRequest request) {
        return execute(() -> articleService.updateStock(id, request.getStock()));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete article", description = "Deletes an article (ADMIN only)")
    @APIResponse(responseCode = "204", description = "Article deleted")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden - Admin role required")
    @APIResponse(responseCode = "404", description = "Article not found")
    public Response deleteArticle(@PathParam("id") Long id) {
        execute(() -> {
            articleService.delete(id);
            return null;
        });
        return Response.noContent().build();
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (ArticleApplicationException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.status(ex.getStatusCode()).build());
        }
    }
}
