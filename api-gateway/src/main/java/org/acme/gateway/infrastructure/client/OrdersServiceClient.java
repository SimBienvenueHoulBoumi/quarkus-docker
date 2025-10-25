package org.acme.gateway.infrastructure.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/orders")
public interface OrdersServiceClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Object getAllOrders(@HeaderParam("Authorization") String authorization, 
                       @QueryParam("page") Integer page, 
                       @QueryParam("size") Integer size);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Object getOrderById(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Object createOrder(@HeaderParam("Authorization") String authorization, Object request);

    @PATCH
    @Path("/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    Object updateOrderStatus(@HeaderParam("Authorization") String authorization,
                             @PathParam("id") Long id,
                             @QueryParam("status") String status);
}
