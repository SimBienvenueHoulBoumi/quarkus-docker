package org.acme.orders.web;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.orders.domain.OrderStatus;
import org.acme.orders.service.OrderService;
import org.acme.orders.web.dto.CreateOrderRequest;
import org.acme.orders.web.dto.OrderResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    OrderService orderService;

    @POST
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Create a new order", description = "Create a new order for the authenticated user")
    public Response createOrder(@Valid CreateOrderRequest request) {
        Long userId = getUserIdFromToken();
        OrderResponse order = orderService.createOrder(userId, request);
        return Response.status(Response.Status.CREATED).entity(order).build();
    }

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get my orders", description = "Get all orders for the authenticated user")
    public List<OrderResponse> getMyOrders() {
        Long userId = getUserIdFromToken();
        return orderService.getUserOrders(userId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get order by ID", description = "Get a specific order by ID for the authenticated user")
    public OrderResponse getOrderById(@PathParam("id") Long id) {
        Long userId = getUserIdFromToken();
        return orderService.getOrderById(id, userId);
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Update order status", description = "Update the status of an order")
    public OrderResponse updateOrderStatus(
            @PathParam("id") Long id,
            @QueryParam("status") OrderStatus status) {
        
        if (status == null) {
            throw new WebApplicationException("Status parameter is required", Response.Status.BAD_REQUEST);
        }
        
        Long userId = getUserIdFromToken();
        return orderService.updateOrderStatus(id, userId, status);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public Response cancelOrder(@PathParam("id") Long id) {
        Long userId = getUserIdFromToken();
        orderService.cancelOrder(id, userId);
        return Response.noContent().build();
    }

    private Long getUserIdFromToken() {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new WebApplicationException("Missing userId claim", Response.Status.UNAUTHORIZED);
        }
        return Long.valueOf(claim.toString());
    }
}
