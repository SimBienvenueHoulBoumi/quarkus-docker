package org.acme.articles.application.exception;

public class ArticleApplicationException extends RuntimeException {

    private final int statusCode;

    public ArticleApplicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
