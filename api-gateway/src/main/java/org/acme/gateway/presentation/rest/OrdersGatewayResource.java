package org.acme.gateway.presentation.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.acme.gateway.infrastructure.client.OrdersServiceClient;
import org.acme.gateway.application.dto.request.OrderStatusUpdateRequest;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdersGatewayResource {

    @Inject
    @RestClient
    OrdersServiceClient ordersServiceClient;

    @GET
    public Object getAllOrders(@Context HttpHeaders headers,
                              @QueryParam("page") Integer page,
                              @QueryParam("size") Integer size) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return ordersServiceClient.getAllOrders(authorization, page, size);
    }

    @GET
    @Path("/{id}")
    public Object getOrderById(@Context HttpHeaders headers, @PathParam("id") Long id) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return ordersServiceClient.getOrderById(authorization, id);
    }

    @POST
    public Object createOrder(@Context HttpHeaders headers, Object request) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return ordersServiceClient.createOrder(authorization, request);
    }

    @PATCH
    @Path("/{id}/status")
    public Object updateOrderStatus(@Context HttpHeaders headers,
                                    @PathParam("id") Long id,
                                    @Valid OrderStatusUpdateRequest request) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        String status = request.status().trim();
        return ordersServiceClient.updateOrderStatus(authorization, id, status.toUpperCase());
    }
}
