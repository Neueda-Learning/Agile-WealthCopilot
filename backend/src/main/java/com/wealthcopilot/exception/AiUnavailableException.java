package com.wealthcopilot.exception;

/** Maps to 503 AI_UNAVAILABLE — the LLM provider is unreachable or failing. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
