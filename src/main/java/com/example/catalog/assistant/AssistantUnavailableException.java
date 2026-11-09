package com.example.catalog.assistant;

/** The chat call failed; mapped to REACTIVE's 500 "Assistant unavailable: <message>" response. */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
