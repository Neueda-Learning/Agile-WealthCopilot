package com.wealthcopilot.news;

public class NewsClientException extends RuntimeException {

    public NewsClientException(String message) {
        super(message);
    }

    public NewsClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
