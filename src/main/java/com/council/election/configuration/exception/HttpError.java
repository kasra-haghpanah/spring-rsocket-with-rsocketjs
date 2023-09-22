package com.council.election.configuration.exception;

public class HttpError {

    private String message;

    public HttpError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
