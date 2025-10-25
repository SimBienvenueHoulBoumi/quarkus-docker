package org.acme.orders.presentation.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.orders.application.OrderService;
import org.acme.orders.application.exception.OrderApplicationException;
import org.acme.orders.domain.model.OrderStatus;
import org.acme.orders.application.dto.request.CreateOrderRequest;
import org.acme.orders.application.dto.response.OrderResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.function.Supplier;

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
        OrderResponse order = execute(() -> orderService.createOrder(userId, request));
        return Response.status(Response.Status.CREATED).entity(order).build();
    }

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get my orders", description = "Get all orders for the authenticated user")
    public List<OrderResponse> getMyOrders() {
        Long userId = getUserIdFromToken();
        return execute(() -> orderService.getUserOrders(userId));
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get order by ID", description = "Get a specific order by ID for the authenticated user")
    public OrderResponse getOrderById(@PathParam("id") Long id) {
        Long userId = getUserIdFromToken();
        return execute(() -> orderService.getOrderById(id, userId));
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update order status", description = "Update the status of an order")
    public OrderResponse updateOrderStatus(
            @PathParam("id") Long id,
            @QueryParam("status") OrderStatus status) {
        
        if (status == null) {
            throw new WebApplicationException("Status parameter is required", Response.Status.BAD_REQUEST);
        }
        
        return execute(() -> orderService.updateOrderStatus(id, status));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public Response cancelOrder(@PathParam("id") Long id) {
        Long userId = getUserIdFromToken();
        execute(() -> {
            orderService.cancelOrder(id, userId);
            return null;
        });
        return Response.noContent().build();
    }

    private Long getUserIdFromToken() {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new WebApplicationException("Missing userId claim", Response.Status.UNAUTHORIZED);
        }
        return Long.valueOf(claim.toString());
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (OrderApplicationException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.status(ex.getStatusCode()).build());
        }
    }
}
