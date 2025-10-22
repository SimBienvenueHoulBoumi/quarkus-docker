package org.acme.users.application.exception;

public class UserApplicationException extends RuntimeException {

    private final int statusCode;

    public UserApplicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
