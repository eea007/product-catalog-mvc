package com.example.catalog.service;

/** A downstream dependency failed and the failure is surfaced to the caller; mapped to 503. */
public class DownstreamServiceException extends RuntimeException {

    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
