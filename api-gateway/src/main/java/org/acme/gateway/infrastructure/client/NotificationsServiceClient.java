package org.acme.gateway.infrastructure.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/notifications")
public interface NotificationsServiceClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Object getAllNotifications(@HeaderParam("Authorization") String authorization,
                              @QueryParam("page") Integer page,
                              @QueryParam("size") Integer size);

    @GET
    @Path("/unread")
    @Produces(MediaType.APPLICATION_JSON)
    Object getUnreadNotifications(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/unread/count")
    @Produces(MediaType.APPLICATION_JSON)
    Object getUnreadCount(@HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}/read")
    @Produces(MediaType.APPLICATION_JSON)
    Object markAsRead(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id);
}
