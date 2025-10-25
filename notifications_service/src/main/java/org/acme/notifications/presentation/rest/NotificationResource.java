package org.acme.notifications.presentation.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.notifications.application.NotificationService;
import org.acme.notifications.application.exception.NotificationApplicationException;
import org.acme.notifications.application.dto.response.NotificationResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    NotificationService notificationService;

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get all notifications", description = "Get all notifications for the authenticated user")
    public List<NotificationResponse> getAllNotifications() {
        Long userId = getUserIdFromToken();
        return execute(() -> notificationService.getUserNotifications(userId));
    }

    @GET
    @Path("/unread")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get unread notifications", description = "Get all unread notifications for the authenticated user")
    public List<NotificationResponse> getUnreadNotifications() {
        Long userId = getUserIdFromToken();
        return execute(() -> notificationService.getUnreadNotifications(userId));
    }

    @GET
    @Path("/unread/count")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications")
    public Map<String, Long> getUnreadCount() {
        Long userId = getUserIdFromToken();
        long count = execute(() -> notificationService.getUnreadCount(userId));
        return Map.of("unreadCount", count);
    }

    @PATCH
    @Path("/{id}/read")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public NotificationResponse markAsRead(@PathParam("id") Long id) {
        Long userId = getUserIdFromToken();
        return execute(() -> notificationService.markAsRead(id, userId));
    }

    @PATCH
    @Path("/read-all")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read for the authenticated user")
    public Response markAllAsRead() {
        Long userId = getUserIdFromToken();
        execute(() -> {
            notificationService.markAllAsRead(userId);
            return null;
        });
        return Response.ok(Map.of("message", "All notifications marked as read")).build();
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
        } catch (NotificationApplicationException ex) {
            throw new WebApplicationException(ex.getMessage(), Response.status(ex.getStatusCode()).build());
        }
    }
}
