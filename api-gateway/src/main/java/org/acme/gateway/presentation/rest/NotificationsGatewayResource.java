package org.acme.gateway.presentation.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.acme.gateway.infrastructure.client.NotificationsServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationsGatewayResource {

    @Inject
    @RestClient
    NotificationsServiceClient notificationsServiceClient;

    @GET
    public Object getAllNotifications(@Context HttpHeaders headers,
                                     @QueryParam("page") Integer page,
                                     @QueryParam("size") Integer size) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return notificationsServiceClient.getAllNotifications(authorization, page, size);
    }

    @GET
    @Path("/unread")
    public Object getUnreadNotifications(@Context HttpHeaders headers) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return notificationsServiceClient.getUnreadNotifications(authorization);
    }

    @GET
    @Path("/unread/count")
    public Object getUnreadCount(@Context HttpHeaders headers) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return notificationsServiceClient.getUnreadCount(authorization);
    }

    @PATCH
    @Path("/{id}/read")
    public Object markAsRead(@Context HttpHeaders headers, @PathParam("id") Long id) {
        String authorization = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return notificationsServiceClient.markAsRead(authorization, id);
    }
}
