package org.acme.gateway.infrastructure.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/q/openapi")
public interface OpenApiClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    String getOpenApi();
}
