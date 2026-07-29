package com.wealthcopilot.llm;

/** A tool invocation requested by the model; arguments arrive as a JSON string. */
public record LlmToolCall(String id, String name, String argumentsJson) {
}
