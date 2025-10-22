package org.acme.notifications.application.exception;

public class NotificationApplicationException extends RuntimeException {

    private final int statusCode;

    public NotificationApplicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
