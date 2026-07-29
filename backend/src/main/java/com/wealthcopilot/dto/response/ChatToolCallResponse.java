package com.wealthcopilot.dto.response;

/** Disclosure of what the agent actually read, shown under its reply. */
public record ChatToolCallResponse(String name, long durationMs) {
}
