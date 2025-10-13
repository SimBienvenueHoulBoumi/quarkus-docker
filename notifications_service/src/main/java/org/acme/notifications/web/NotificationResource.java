package org.acme.notifications.web;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.notifications.service.NotificationService;
import org.acme.notifications.web.dto.NotificationResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

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
        return notificationService.getUserNotifications(userId);
    }

    @GET
    @Path("/unread")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get unread notifications", description = "Get all unread notifications for the authenticated user")
    public List<NotificationResponse> getUnreadNotifications() {
        Long userId = getUserIdFromToken();
        return notificationService.getUnreadNotifications(userId);
    }

    @GET
    @Path("/unread/count")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications")
    public Map<String, Long> getUnreadCount() {
        Long userId = getUserIdFromToken();
        long count = notificationService.getUnreadCount(userId);
        return Map.of("unreadCount", count);
    }

    @PATCH
    @Path("/{id}/read")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public NotificationResponse markAsRead(@PathParam("id") Long id) {
        Long userId = getUserIdFromToken();
        return notificationService.markAsRead(id, userId);
    }

    @PATCH
    @Path("/read-all")
    @RolesAllowed({"USER", "ADMIN"})
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read for the authenticated user")
    public Response markAllAsRead() {
        Long userId = getUserIdFromToken();
        notificationService.markAllAsRead(userId);
        return Response.ok(Map.of("message", "All notifications marked as read")).build();
    }

    private Long getUserIdFromToken() {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new WebApplicationException("Missing userId claim", Response.Status.UNAUTHORIZED);
        }
        return Long.valueOf(claim.toString());
    }
}
