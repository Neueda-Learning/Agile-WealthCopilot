package com.wealthcopilot.exception;

/** Maps to 422 AI_PARSE_FAILED — the input could not be turned into a complete draft. */
public class AiParseFailedException extends RuntimeException {

    public AiParseFailedException(String message) {
        super(message);
    }
}
