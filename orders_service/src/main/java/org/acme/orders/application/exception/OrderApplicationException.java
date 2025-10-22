package org.acme.orders.application.exception;

public class OrderApplicationException extends RuntimeException {

    private final int statusCode;

    public OrderApplicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
